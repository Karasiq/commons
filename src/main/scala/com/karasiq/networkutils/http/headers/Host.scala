package com.karasiq.networkutils.http.headers

import java.net.InetSocketAddress

import com.karasiq.networkutils.url.URLParser

/**
 * `Host` HTTP header
 */
object Host extends HttpHeader.Extractor("Host") {
  private val regex = """(\b(?:https?://|)[^\s]+)""".r

  override def apply(s: String): HttpHeader = {
    val url = URLParser.withDefaultProtocol(s)
    HttpHeader(name, if (url.getPort == 80 || url.getPort == -1) url.getHost else url.getHost + ":" + url.getPort.toString)
  }

  def apply(address: InetSocketAddress): HttpHeader = {
    if (address.getPort == 80) HttpHeader(name, address.getHostString)
    else HttpHeader(name, address.toString)
  }

  override def unapply(h: HttpHeader): Option[String] = h match {
    case HttpHeader(`name`, regex(host)) ⇒
      Some(host)

    case _ ⇒
      None
  }
}
