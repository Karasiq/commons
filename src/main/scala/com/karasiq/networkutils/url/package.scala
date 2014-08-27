package com.karasiq.networkutils

import java.net.{URI, URL}

import com.karasiq.common.factory.ParametrizedFactory

package object url {
  trait URLProvider[T] extends ParametrizedFactory[T, URL]

  implicit object StringURLProvider extends URLProvider[String] {
    override def apply(src: String): URL = new URL(src)
  }

  implicit object URLURLProvider extends URLProvider[URL] {
    override def apply(src: URL): URL = src
  }

  implicit object URIURLProvider extends URLProvider[URI] {
    override def apply(src: URI): URL = src.toURL
  }

  implicit object URLParserURLProvider extends URLProvider[URLParser] {
    override def apply(src: URLParser): URL = src.toURL
  }

  def asURL[T](src: T)(implicit toUrl: URLProvider[T]): URL = {
    toUrl(src)
  }
}
