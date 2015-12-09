package com.karasiq.networkutils.proxy

import java.net.{InetSocketAddress, URI}

import com.karasiq.networkutils.proxy.Proxy.ProxyURIProvider
import com.karasiq.networkutils.uri.URIProvider
import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import scala.util.control.Exception

abstract class Proxy {
  def host: String
  def port: Int
  def scheme: String
  def userInfo: Option[String]


  override def toString: String = {
    ProxyURIProvider(this).toString
  }

  final def toJavaProxy: java.net.Proxy = {
    val t = scheme match {
      case "socks" | "socks4" | "socks5" ⇒
        java.net.Proxy.Type.SOCKS

      case "http" | "https" | "" | null ⇒
        java.net.Proxy.Type.HTTP

      case sch ⇒
        throw new IllegalArgumentException(s"Unsupported proxy scheme: $sch")
    }
    val a = this.toInetSocketAddress
    new java.net.Proxy(t, a)
  }

  final def toInetSocketAddress: InetSocketAddress = new InetSocketAddress(host, port)
}

/**
 * Loads settings from config
 * @param config Configuration object
 * @example {{{
 *         /* application.conf:
 *           my-proxy {
 *             host = 127.0.0.1
 *             port = 1080
 *             scheme = "socks"
 *           }
 *         */
 *
 *         // Load configuration
 *         val config = ConfigFactory.load()
 *
 *         // Create proxy
 *         val proxy = Proxy.config(config, "my-proxy")
 * }}}
 */
final class TypeSafeConfigProxy(config: Config) extends Proxy {
  override val host: String = config.getString("host")

  override val scheme: String = config.getString("scheme")

  override val port: Int = config.getInt("port")

  override val userInfo: Option[String] = Exception.catching(classOf[ConfigException.Missing]).opt(config.getString("userinfo"))
}

object Proxy {
  /**
   * Default proxy configuration key
   */
  private def defaultProxyConfigKey: String = "karasiq.http.default-proxy"

  /**
   * Default proxy from config key [[defaultProxyConfigKey]]
   */
  val default: Option[Proxy] = Some(this.config(defaultProxyConfigKey))
    .filter(p ⇒ p.host.nonEmpty && p.port > 0)

  def apply[U](uri: U)(implicit toUri: URIProvider[U]): Proxy = {
    apply(uri.getHost, uri.getPort, uri.getScheme, Option(uri.getUserInfo))
  }

  @throws[IllegalArgumentException]("if invalid configuration provided")
  def apply(proxyHost: String, proxyPort: Int, proxyScheme: String = "http", proxyUserinfo: Option[String] = None): Proxy = {
    if(proxyHost.isEmpty || !(1 to 65535).contains(proxyPort) || proxyScheme.isEmpty) throw new IllegalArgumentException("Invalid proxy configuration")

    new Proxy {
      override def host: String = proxyHost

      override def scheme: String = proxyScheme

      override def port: Int = proxyPort

      override def userInfo: Option[String] = proxyUserinfo
    }
  }

  def unapply(p: Proxy): Option[(String, String, Int, Option[String])] = {
    Some((p.scheme, p.host, p.port, p.userInfo))
  }

  implicit object ProxyURIProvider extends URIProvider[Proxy] {
    override def apply(v1: Proxy): URI = {
      new URI(v1.scheme, v1.userInfo.orNull, v1.host, v1.port, null, null, null)
    }
  }

  def config(c: Config): Proxy = new TypeSafeConfigProxy(c)

  def config(c: Config, key: String): Proxy = config(c.getConfig(key))

  def config(key: String): Proxy = config(ConfigFactory.load(), key)
}
