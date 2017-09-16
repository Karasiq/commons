package com.karasiq.networkutils.cloudflare

import java.net.URL
import java.util.concurrent.{Executors, TimeoutException, TimeUnit}

import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.language.postfixOps

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.util.Cookie

import com.karasiq.networkutils.HtmlUnitUtils._

object CloudFlareCookieRetriever {
  def apply(createWebClient: () ⇒ WebClient = () ⇒ CloudFlareUtils.compatibleWebClient()): CloudFlareCookieRetriever = {
    new CloudFlareCookieRetrieverImpl(createWebClient)
  }
}

trait CloudFlareCookieRetriever {
  def retrieveCookiesAsync(url: URL): Future[Set[Cookie]]
  def retrieveCookies(url: URL): Set[Cookie] = Await.result(retrieveCookiesAsync(url), 1 minute)
}

private final class CloudFlareListener(url: URL) {
  val promise: Promise[Set[Cookie]] = Promise[Set[Cookie]]()

  def future: Future[Set[Cookie]] = promise.future

  val listener: WebWindowListener = new WebWindowListener {
    override def webWindowContentChanged(p1: WebWindowEvent): Unit = {
      if (p1.getEventType == WebWindowEvent.CHANGE && p1.getNewPage != null && p1.getOldPage != null && p1.getOldPage.getUrl == p1.getNewPage.getUrl) {
        val webClient = p1.getWebWindow.getWebClient
        webClient.closeAfter {
          webClient.removeWebWindowListener(this)
          val cookies = webClient.getCookies(url).asScala.toSet
          promise.success(cookies)
        }
      }
    }

    override def webWindowClosed(p1: WebWindowEvent): Unit = {}

    override def webWindowOpened(p1: WebWindowEvent): Unit = {}
  }

  def timeout(): Unit = {
    if (!promise.isCompleted) promise.failure(new TimeoutException("Cloudflare event timed out"))
  }
}

private final class CloudFlareCookieRetrieverImpl(createWebClient: () ⇒ WebClient) extends CloudFlareCookieRetriever {
  private[this] val scheduler = Executors.newSingleThreadScheduledExecutor()
  private[this] implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(scheduler)

  override def retrieveCookiesAsync(url: URL): Future[Set[HtmlUnitCookie]] = {
    val webClient = createWebClient()
    val handler = new CloudFlareListener(url)
    webClient.addWebWindowListener(handler.listener)
    val page = concurrent.blocking(webClient.getPage[Page](url))

    // Timeout in 15 seconds
    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        webClient.removeWebWindowListener(handler.listener)
        webClient.close()
        handler.timeout()
      }
    }, 15, TimeUnit.SECONDS)

    if (!CloudFlareUtils.isCloudFlarePage(page)) {
      val cookies = webClient.cookies(url)
      Future.successful(cookies)
    } else {
      handler.future
    }
  }

  override def finalize(): Unit = {
    scheduler.shutdown()
    super.finalize()
  }
}
