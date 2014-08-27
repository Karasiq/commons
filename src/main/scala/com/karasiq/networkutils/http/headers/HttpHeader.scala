package com.karasiq.networkutils.http.headers

import scala.language.implicitConversions

case class HttpHeader(name: String, value: String)

object HttpHeader {
  class Extractor(val name: String) {
    def apply(value: String): HttpHeader = HttpHeader(name, value)
    def unapply(header: HttpHeader): Option[String] = header match {
      case HttpHeader(`name`, value) ⇒
        Some(value)

      case _ ⇒
        None
    }
  }

  def unapply(s: String): Option[HttpHeader] = s.split(": ", 2).toList match {
    case name :: value :: Nil ⇒
      Some(HttpHeader(name, value))

    case _ ⇒
      None
  }

  def apply(s: String): HttpHeader = unapply(s).getOrElse(throw new IllegalArgumentException("Not HTTP header: " + s))

  def apply(kv: (String, String)): HttpHeader = HttpHeader(kv._1, kv._2)

  def formatHeaders(headers: Seq[HttpHeader]): String = {
    if (headers.nonEmpty) headers.map(h ⇒ s"${h.name}: ${h.value}").mkString("", "\r\n", "\r\n")
    else ""
  }

  implicit def mapToHttpHeaders(hs: Map[String, String]): Seq[HttpHeader] = hs.map(HttpHeader.apply).toSeq
  implicit def httpHeadersToMap(hs: Seq[HttpHeader]): Map[String, String] = hs.map(h ⇒ h.name → h.value).toMap
}