package com.karasiq.networkutils.proxy

import java.net.{InetSocketAddress, URI, Proxy ⇒ JavaProxy}

import scala.util.control.{Exception ⇒ ExcControl}

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import com.karasiq.networkutils.proxy.Proxy.ProxyURIProvider
import com.karasiq.networkutils.uri.URIProvider

trait Proxy {
  def host: String
  def port: Int
  def scheme: String
  def userInfo: Option[String]

  override def toString: String = {
    ProxyURIProvider(this).toString
  }

  def toJavaProxy: JavaProxy = {
    val javaProxyType = scheme match {
      case "" if port == 0 ⇒
        JavaProxy.Type.DIRECT

      case "socks" | "socks4" | "socks5" ⇒
        JavaProxy.Type.SOCKS

      case "http" | "https" | "" | null ⇒
        JavaProxy.Type.HTTP

      case scheme ⇒
        throw new IllegalArgumentException(s"Unsupported proxy scheme: $scheme")
    }

    val proxyAddress = this.toInetSocketAddress
    new JavaProxy(javaProxyType, proxyAddress)
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
 *             scheme = socks
 *           }
 *
 *           // Or
 *           my-proxy.url = socks://127.0.0.1:1080
 *         */
 *
 *         // Load configuration
 *         val config = ConfigFactory.load()
 *
 *         // Create proxy
 *         val proxy = Proxy.config(config, "my-proxy")
 * }}}
 */
private[proxy] final class TypeSafeConfigProxy(config: Config) extends Proxy {
  private[this] val uri = ExcControl.catching(classOf[ConfigException.Missing])
    .opt(config.getString("url"))
    .map(url ⇒ new URI(if (url.contains("://")) url else "http://" + url))

  override val host: String = uri.fold(config.getString("host"))(_.getHost)
  override val scheme: String = uri.fold(config.getString("scheme"))(_.getScheme)
  override val port: Int = uri.fold(config.getInt("port"))(_.getPort)
  override val userInfo: Option[String] = uri.map(_.getUserInfo)
    .orElse(ExcControl.catching(classOf[ConfigException.Missing]).opt(config.getString("userinfo")))
}

object Proxy {
  /**
    * Default configuration
    */
  private lazy val defaultConfig = ConfigFactory.load()

  /**
   * Default proxy configuration key
   */
  private def defaultProxyConfigKey: String = "karasiq.http.default-proxy"

  /**
   * Default proxy from config
   */
  lazy val default: Option[Proxy] = ExcControl.catching(classOf[ConfigException])
    .opt(this.config(defaultProxyConfigKey))
    .filter(p ⇒ p.host.nonEmpty && p.port > 0)

  def apply[U](uri: U)(implicit toUri: URIProvider[U]): Proxy = {
    val _uri: URI = toUri(uri)
    apply(_uri.getHost, _uri.getPort, _uri.getScheme, Option(_uri.getUserInfo))
  }

  @throws[IllegalArgumentException]("if invalid configuration provided")
  def apply(proxyHost: String, proxyPort: Int, proxyScheme: String = "http", proxyUserinfo: Option[String] = None): Proxy = {
    if(proxyHost.isEmpty || !(1 to 65535).contains(proxyPort) || proxyScheme.isEmpty)
      throw new IllegalArgumentException("Invalid proxy configuration")

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
    override def apply(proxy: Proxy): URI = {
      new URI(proxy.scheme, proxy.userInfo.orNull, proxy.host, proxy.port, null, null, null)
    }
  }

  def config(c: Config): Proxy = new TypeSafeConfigProxy(c)
  def config(c: Config, key: String): Proxy = config(c.getConfig(key))
  def config(key: String): Proxy = config(defaultConfig, key)
}
