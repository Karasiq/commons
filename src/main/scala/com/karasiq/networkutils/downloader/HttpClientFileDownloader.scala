package com.karasiq.networkutils.downloader

import java.io.{Closeable, InputStream}

import com.karasiq.common.Lazy
import com.karasiq.networkutils.HttpClientUtils._
import com.karasiq.networkutils.http
import com.karasiq.networkutils.http.HttpStatus
import com.karasiq.networkutils.http.headers.HttpHeader
import org.apache.commons.io.IOUtils
import org.apache.http.client.HttpClient
import org.apache.http.client.methods._
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.{HttpRequest, HttpResponse}

/**
 * Downloads files with HttpClient
 * @example fileDownloader ! FileToDownload("http://example.com/file.jpg", "files", "saved_file.jpg")
 */
class HttpClientFileDownloader(httpClientBuilder: HttpClientBuilder) extends FileDownloader {
  def this() = this(defaultSettings.builder)

  protected def createHttpClientBuilder(): HttpClientBuilder = {
    httpClientBuilder
  }

  abstract class HttpConnection extends Closeable {
    def httpClient: HttpClient
    def request: HttpRequest
    def response: HttpResponse
  }

  protected class HttpConnectionImpl(val httpClient: HttpClient, val request: HttpUriRequest) extends HttpConnection {
    protected def executeRequest() = httpClient.execute(request)

    private val response_ : Lazy[HttpResponse] = Lazy(executeRequest())

    override def response: HttpResponse = response_()

    def close(): Unit = {
      /* request match {
        case hrb: HttpRequestBase ⇒
          hrb.releaseConnection()
        case _ ⇒
      } */

      response_.ifDefined {
        case c: Closeable ⇒ IOUtils.closeQuietly(c)
        case _ ⇒
      }
    }
  }

  protected def openHttpConnection(httpClient: HttpClient, request: HttpUriRequest): HttpConnection = {
    new HttpConnectionImpl(httpClient, request)
  }

  protected def prepareRequest(request: HttpUriRequest, headers: Seq[HttpHeader]): HttpUriRequest = {
    request.setHeader("Connection", "Keep-Alive") // For keep-alive
    headers.foreach(h ⇒ request.addHeader(h.name, h.value))
    request
  }
  
  /**
   * Saves the file from given URL
   * @param url Internet address
   */
  override def loadFile(url: String, headers: Seq[HttpHeader], cookies: Traversable[HttpClientCookie]): LoadedFile = {
    val httpClient = this.createHttpClientBuilder().setDefaultCookieStore(cookies.toCookieStore)
      .build()

    def httpGet(url: String) = {
      val request = prepareRequest(new HttpGet(url), headers)
      openHttpConnection(httpClient, request)
    }

    def httpHead(url: String) = {
      val request = prepareRequest(new HttpHead(url), headers)
      openHttpConnection(httpClient, request)
    }

    new LoadedFile {
      private val connection = httpGet(url)

      private val head = httpHead(url)

      override def responseHeaders: Seq[HttpHeader] = head.response.getAllHeaders.map(h ⇒ h.getName → h.getValue).toMap

      override def openStream(): InputStream = connection.response.getEntity.getContent

      override lazy val contentLength: Option[Long] = {
        val header = Option(head.response.getFirstHeader("Content-Length")).map(_.getValue.toLong)
        if (header.nonEmpty) header else Some(connection.response.getEntity.getContentLength)
      }

      override lazy val status: http.HttpStatus = HttpStatus(head.response.getStatusLine.getStatusCode, head.response.getStatusLine.getReasonPhrase)

      override def close(): Unit = {
        head.close()
        connection.close()
      }
    }
  }

  override def close(): Unit = ()
}
