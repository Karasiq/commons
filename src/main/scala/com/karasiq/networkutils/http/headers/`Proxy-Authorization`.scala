package com.karasiq.networkutils.http.headers

import java.util.Base64

import scala.util.matching.Regex

/**
 * `Proxy-Authorization` HTTP header
 */
object `Proxy-Authorization` extends HttpHeader.Extractor("Proxy-Authorization") {

  /**
   * Basic authorization type
   * @see [[https://tools.ietf.org/html/rfc1945#section-11.1]]
   */
  private object Basic {
    private val regex: Regex = "Basic (\\w+)".r

    private def fromBase64(s: String): String = {
      new String(Base64.getDecoder.decode(s))
    }

    private def toBase64(s: String): String = {
      new String(Base64.getEncoder.encode(s.getBytes))
    }

    def unapply(s: String): Option[String] = {
      regex.findFirstMatchIn(s).map(m ⇒ fromBase64(m.group(1)))
    }

    def apply(userInfo: String): String = "Basic " + toBase64(userInfo)
  }

  override def unapply(s: HttpHeader): Option[String] = s match {
    case HttpHeader(`name`, Basic(userInfo)) ⇒
      Some(userInfo)

    // TODO: other auth methods

    case _ ⇒
      None
  }

  def basic(userInfo: String): HttpHeader = {
    HttpHeader(name, Basic(userInfo))
  }

  override def apply(value: String): HttpHeader = basic(value) // Default
}
