package com.karasiq.networkutils.cloudflare

import java.net.URL
import java.util.concurrent.{Executors, TimeUnit, TimeoutException}

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.util.Cookie
import com.karasiq.common.factory.Factory
import com.karasiq.networkutils.HtmlUnitUtils._

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.language.postfixOps

object CloudFlareCookieRetriever {
  def apply(): CloudFlareCookieRetriever = new CloudFlareCookieRetrieverImpl
}

trait CloudFlareCookieRetriever {
  def retrieveCookiesAsync(url: URL): Future[Set[Cookie]]
  def retrieveCookies(url: URL): Set[Cookie] = Await.result(retrieveCookiesAsync(url), Duration.Inf)
}

private final class CloudFlareListener(url: URL) {
  val promise: Promise[Set[Cookie]] = Promise[Set[Cookie]]()

  def future: Future[Set[Cookie]] = promise.future

  val listener: WebWindowListener = new WebWindowListener {
    override def webWindowContentChanged(p1: WebWindowEvent): Unit = {
      if (p1.getEventType == WebWindowEvent.CHANGE && p1.getNewPage != null && p1.getOldPage != null && p1.getOldPage.getUrl == p1.getNewPage.getUrl) {
        val webClient = p1.getWebWindow.getWebClient
        try {
          webClient.removeWebWindowListener(this)
          val cookies = webClient.getCookies(url).toSet
          // log.info(s"CloudFlare cookies dumped: $cookies")
          promise.success(cookies)
        } finally {
          webClient.closeAllWindows()
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

final private class CloudFlareCookieRetrieverImpl extends CloudFlareCookieRetriever {
  private val scheduler = Executors.newSingleThreadScheduledExecutor()

  private object WebClientFactory extends Factory[WebClient] {
    val cookieManager = new CookieManager
    val cache = new Cache
    override def apply(): WebClient = CloudFlareUtils.compatibleWebClient(cache = cache, cookieManager = cookieManager)
  }

  override def retrieveCookiesAsync(url: URL) = {
    val webClient = WebClientFactory()
    val handler = new CloudFlareListener(url)
    webClient.addWebWindowListener(handler.listener)
    val page = webClient.getPage[Page](url)

    // Timeout in 15 seconds
    scheduler.schedule(new Runnable {
      override def run(): Unit = {
        webClient.removeWebWindowListener(handler.listener)
        webClient.closeAllWindows()
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
