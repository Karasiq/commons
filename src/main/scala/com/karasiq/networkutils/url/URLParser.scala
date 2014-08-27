package com.karasiq.networkutils.url

import java.net.{MalformedURLException, URI, URL, URLDecoder}

import com.karasiq.networkutils.url
import org.apache.commons.io.{Charsets, FilenameUtils}
import org.apache.http.NameValuePair
import org.apache.http.client.utils.{URIBuilder, URLEncodedUtils}
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConversions._
import scala.util.control
import scala.util.matching.Regex

/**
 * URL query parser util
 */
sealed abstract class QueryParser {
  /**
   * Returns name-value [[Map]]
   */
  def toMap: Map[String, String]

  /**
   * Returns name-value tuple seq
   */
  def toSeq: Seq[(String, String)] = toMap.toSeq

  /**
   * Returns sequence of [[org.apache.http.NameValuePair NameValuePair]]
   */
  private[networkutils] def toNameValuePairSeq: Seq[NameValuePair] = toMap.toVector.map { case (k, v) => new BasicNameValuePair(k, v.toString) }

  /**
   * Creates query with new parameter
   * @param kv Name, value
   * @return Modified query
   */
  def +(kv: (String, String)): QueryParser = QueryParser(toMap + kv)

  /**
   * Creates query with new parameters
   * @param kv Name, value list
   * @return Modified query
   */
  def ++(kv: Iterable[(String, String)]): QueryParser = QueryParser(toMap ++ kv)

  /**
   * Formats to URL-encoded query string
   * @example {{{
   *         val query = QueryParser("key1" -> "value 1", "key2" -> "value 2")
   *         println(query.mkString) // Outputs "key1=value%201&key2=value%202"
   * }}}
   */
  def mkString: String = {
    URLEncodedUtils.format(toNameValuePairSeq, '&', "UTF-8")
  }
}

object QueryParser {
  def apply(map: Map[String, String]): QueryParser = new QueryParser {
    override def toMap: Map[String, String] = map
  }

  def apply(params: (String, String)*): QueryParser = apply(params.toMap)

  private[networkutils] def apply(params: Iterable[NameValuePair]): QueryParser = apply(params.map(kv => kv.getName -> kv.getValue).toMap)

  def apply(query: String): QueryParser = apply(URLEncodedUtils.parse(query, Charsets.UTF_8))
}

/**
 * URL file path parser util
 */
sealed abstract class URLFilePathParser {
  /**
   * Extracts file path from URL
   */
  def path: String

  /**
   * Extracts file name from URL
   */
  def name: String

  /**
   * Extracts file extension from URL
   */
  def extension = FilenameUtils.getExtension(this.name)
}

object URLFilePathParser {
  private val pathSeparator: String = "/"

  private def parsePath(fullPath: String): Seq[String] = {
    fullPath.split(Regex.quote(pathSeparator)).filter(_.nonEmpty)
  }

  private def parsePathRemoveLast(fullPath: String): Seq[String] = {
    val path = parsePath(fullPath)
    if (path.nonEmpty) path.dropRight(1) else path
  }

  def withoutFile(fullPath: String): String = {
    parsePathRemoveLast(fullPath).mkString(pathSeparator, pathSeparator, pathSeparator)
  }

  private def pathFileName(filePath: String) = {
    filePath.split(Regex.quote(pathSeparator)).lastOption.getOrElse("")
  }
  
  private def urlPath(url: URL): String = {
    URLDecoder.decode(url.getPath, "UTF-8")
  }

  def apply[T](url: T)(implicit toUrl: URLProvider[T]) = new URLFilePathParser {
    override def path: String = urlPath(toUrl(url))

    override def name: String = pathFileName(this.path)
  }

  /**
   * Creates parser with optional URL file name
   * @param url URL to parse
   * @param defaultFileName Fallback file name
   * @return Parser object
   */
  def withDefaultFileName[T](url: T, defaultFileName: String)(implicit toUrl: URLProvider[T]) = new URLFilePathParser {
    private val _url = toUrl(url)

    override def path: String = {
      (parsePathRemoveLast(urlPath(_url)) ++ Traversable(name)).mkString(pathSeparator, pathSeparator, "")
    }

    override def name: String = {
      val urlFileName = pathFileName(urlPath(_url))
      if(defaultFileName.isEmpty) urlFileName
      else if (FilenameUtils.getExtension(defaultFileName).length > 0) defaultFileName
      else s"$defaultFileName.${FilenameUtils.getExtension(urlFileName)}"
    }
  }
}

/**
 * URL parser util
 */
sealed abstract class URLParser {
  def toURL: URL
  def toURI: URI = toURL.toURI
  def toURIBuilder = new URIBuilder(toURL.toURI)
  override def toString: String = toURL.toString

  /**
   * Extracts query from URL
   * @see [[java.net.URL#getQuery]]
   */
  def query: String = toURL.getQuery

  /**
   * Creates URL query parser
   * @see [[url.QueryParser! QueryParser]]
   */
  def queryParser: QueryParser = if (query ne null) QueryParser(query) else QueryParser(Map.empty[String, String])

  /**
   * Creates URL file path parser
   */
  def file: URLFilePathParser = URLFilePathParser(toURL)
  
  /**
   * Extracts protocol from URL
   */
  def protocol: String = toURL.getProtocol

  /**
   * Extracts port from URL
   */
  def port: Int = Some(toURL.getPort).filter(_ != -1).getOrElse {
    protocol match {
      case "http" ⇒
        80
      case "https" ⇒
        443
    }
  }

  /**
   * Extracts URL host
   */
  def host: String = toURL.getHost

  private[networkutils] def withQuery(newQuery: Seq[NameValuePair]): URLParser = {
    URLParser(toURIBuilder.setParameters(newQuery).build())
  }

  def withQuery(newQuery: String): URLParser = {
    withQuery(QueryParser(newQuery))
  }

  def withQuery(newQuery: QueryParser): URLParser = {
    withQuery(newQuery.toNameValuePairSeq)
  }

  def appendQuery(newQuery: (String, String)*): URLParser = {
    withQuery(queryParser ++ newQuery)
  }
}

object URLParser {
  def apply[T](url: T)(implicit toUrl: URLProvider[T]): URLParser = new URLParser {
    override def toURL: URL = toUrl(url)
  }

  /**
   * Checks if URL can be parsed without exception
   * @param url URL
   * @return Is URL valid
   */
  def isValidURL(url: String): Boolean = {
    control.Exception.catching(classOf[MalformedURLException])
      .opt(new URL(url)).nonEmpty
  }

  /**
   * Creates URL with default protocol
   * @param protocol Default protocol
   */
  def withDefaultProtocol(url: String, protocol: String = "http"): URL = {
    control.Exception.catching(classOf[MalformedURLException])
      .opt(new URL(url)).getOrElse(new URL(s"$protocol://$url"))
  }
}