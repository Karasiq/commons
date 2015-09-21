package com.karasiq.networkutils.downloader

import java.io.InputStream

import com.gargoylesoftware.htmlunit.{Page, WebClient, WebRequest}
import com.karasiq.networkutils.HtmlUnitUtils._
import com.karasiq.networkutils.HttpClientUtils._
import com.karasiq.networkutils.http.HttpStatus
import com.karasiq.networkutils.http.headers.HttpHeader
import com.karasiq.networkutils.url._

import scala.collection.JavaConversions._

class HtmlUnitFileDownloader(webClientProducer: ⇒ WebClient) extends FileDownloader {
  def this() = this(newWebClient(js = false))

  override def loadFile(url: String, headers: Seq[HttpHeader], cookies: Traversable[HttpClientCookie]): LoadedFile = {
    val webClient = webClientProducer
    webClient.addCookies(cookies.toHtmlUnit)

    val request = new WebRequest(asURL(url))
    headers.foreach(h ⇒ request.setAdditionalHeader(h.name, h.value))
    val page = webClient.getPage[Page](request)

    new LoadedFile {
      override def responseHeaders: Seq[HttpHeader] = page.getWebResponse.getResponseHeaders.map(kvp ⇒ kvp.getName → kvp.getValue).toMap

      override def openStream(): InputStream = page.getWebResponse.getContentAsStream

      override def contentLength: Option[Long] = page.responseHeader("Content-Length").map(_.toLong).filter(_ > 0)

      override val status: HttpStatus = HttpStatus(page.getWebResponse.getStatusCode, page.getWebResponse.getStatusMessage)

      override def close(): Unit = webClient.close()
    }
  }

  override def close(): Unit = ()
}
