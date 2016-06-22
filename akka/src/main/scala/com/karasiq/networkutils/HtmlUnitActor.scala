package com.karasiq.networkutils

import java.io.IOException
import java.net.MalformedURLException

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.CircuitBreaker
import com.gargoylesoftware.htmlunit.{FailingHttpStatusCodeException, Page, WebClient}
import com.karasiq.networkutils.HtmlUnitUtils._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.Exception
import scala.util.{Failure, Success}

trait PageURL {
  def url: String
}
case class GetPage(url: String) extends PageURL
abstract class HtmlUnitActor(webClientProducer: ⇒ WebClient) extends Actor with ActorLogging {
  import context.dispatcher
  def this() = this(newWebClient(js = false, redirect = false))

  protected val breaker = new CircuitBreaker(context.system.scheduler, maxFailures = 5, callTimeout = 1 minute, resetTimeout = 5 minutes)
    .onOpen(log.warning("Circuit breaker is open"))

  def processPage(message: Any, sender: ActorRef, page: Page): Unit

  def afterProcess(message: Any, sender: ActorRef, webClient: WebClient): Unit = {
    webClient.close()
  }

  private def onError(e: Throwable): Unit = {
    log.error(e, "Page loading error")
  }

  override def receive = {
    case m: PageURL ⇒
      val webClient = webClientProducer
      val sender = context.sender()
      breaker.withCircuitBreaker(Future(concurrent.blocking(webClient.getPage[Page](m.url)))).onComplete {
        case Success(page) ⇒
          Exception.nonFatalCatch.withApply(onError).andFinally(afterProcess(m, sender, webClient)) {
            processPage(m, sender, page)
          }
        case Failure(e) ⇒
          onError(e)
          afterProcess(m, sender, webClient)
      }

    case m ⇒ log.warning("Unknown message: {}", m)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(3, withinTimeRange = 6 minutes) {
    case _: IOException | _: FailingHttpStatusCodeException | _: MalformedURLException ⇒
      Restart
  }
}
