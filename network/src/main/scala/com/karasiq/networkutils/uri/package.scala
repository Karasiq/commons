package com.karasiq.networkutils

import java.net.{URI, URL}

import com.karasiq.common.factory._
import com.karasiq.networkutils.url.URLParser

package object uri {
  trait URIProvider[T] extends ParametrizedFactory[T, URI]

  implicit object StringURIProvider extends URIProvider[String] {
    override def apply(v1: String): URI = new URI(v1)
  }

  implicit object URIURIProvider extends URIProvider[URI] {
    override def apply(v1: URI): URI = v1
  }

  implicit object URLURIProvider extends URIProvider[URL] {
    override def apply(v1: URL): URI = v1.toURI
  }

  implicit object URLParserURIProvider extends URIProvider[URLParser] {
    override def apply(src: URLParser): URI = src.toURI
  }

  def asURI[T](src: T)(implicit toUri: URIProvider[T]): URI = {
    toUri(src)
  }
}
