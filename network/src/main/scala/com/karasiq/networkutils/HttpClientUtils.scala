package com.karasiq.networkutils

import com.karasiq.networkutils.HtmlUnitUtils.HtmlUnitCookie
import com.karasiq.networkutils.proxy.Proxy
import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.{ConnectionKeepAliveStrategy, HttpClientConnectionManager}
import org.apache.http.impl.client.{BasicCookieStore, DefaultHttpRequestRetryHandler, HttpClientBuilder}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHost, HttpResponse}

import scala.collection.JavaConversions._
import scala.language.implicitConversions

object HttpClientUtils {
  type HttpClientCookie = org.apache.http.cookie.Cookie

  trait SettingsContainer {
    def connectionManager: HttpClientConnectionManager
    def requestConfig: RequestConfig
    def retryHandler: HttpRequestRetryHandler
    def keepAlive: ConnectionKeepAliveStrategy

    def builder: HttpClientBuilder = {
      val b = HttpClientBuilder.create()
      if (connectionManager != null) b.setConnectionManager(connectionManager)
      if (requestConfig != null) b.setDefaultRequestConfig(requestConfig)
      if (retryHandler != null) b.setRetryHandler(retryHandler)
      if (keepAlive != null) b.setKeepAliveStrategy(keepAlive)
      b
    }
  }

  case class Settings(connectionManager: HttpClientConnectionManager = null, requestConfig: RequestConfig = null, retryHandler: HttpRequestRetryHandler = null, keepAlive: ConnectionKeepAliveStrategy = null) extends SettingsContainer

  def requestConfig(timeout: Int): RequestConfig = {
    RequestConfig.custom()
      .setProxy(Proxy.default.orNull)
      .setSocketTimeout(timeout)
      .setConnectTimeout(timeout)
      .setConnectionRequestTimeout(timeout)
      .build()
  }

  def poolingConnectionManager(maxTotal: Int = 10, maxPerRoute: Int = 1): PoolingHttpClientConnectionManager = {
    val connectionManager = new PoolingHttpClientConnectionManager
    connectionManager.setMaxTotal(maxTotal)
    connectionManager.setDefaultMaxPerRoute(maxPerRoute)
    connectionManager
  }

  def retryHandler(maxRetries: Int, sentRetryEnabled: Boolean = true) = new DefaultHttpRequestRetryHandler(maxRetries, sentRetryEnabled)

  def keepAlive(ms: Long) = new ConnectionKeepAliveStrategy {
    override def getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long = ms
  }

  def defaultSettings = Settings(poolingConnectionManager(30, 5), requestConfig(30000), retryHandler(3, sentRetryEnabled = true), keepAlive(60000))

  implicit class HttpClientCookiesOps(val cookies: Traversable[HttpClientCookie]) extends AnyVal {
    def toCookieStore: BasicCookieStore = {
      val s = new BasicCookieStore
      cookies.foreach(s.addCookie)
      s
    }

    def toHtmlUnit: Traversable[HtmlUnitCookie] = {
      import com.gargoylesoftware.htmlunit.util.Cookie.{fromHttpClient => convert}
      convert(seqAsJavaList(cookies.toVector)).toTraversable
    }
  }

  implicit def proxyToHttpHost(p: Proxy): HttpHost = {
    import p._
    new HttpHost(host, port, scheme)
  }

  implicit def httpHostToProxy(hh: HttpHost): Proxy = {
    Proxy(hh.getHostName, hh.getPort, hh.getSchemeName)
  }
}
