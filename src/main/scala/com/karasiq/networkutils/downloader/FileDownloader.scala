package com.karasiq.networkutils.downloader

import java.io._

import com.karasiq.fileutils.PathUtils
import com.karasiq.networkutils.HttpClientUtils.{HttpClientCookie => Cookie}
import com.karasiq.networkutils.http.HttpStatus
import com.karasiq.networkutils.http.headers.HttpHeader
import com.karasiq.networkutils.url._
import org.joda.time.DateTime

import scala.language.{implicitConversions, reflectiveCalls}

object FileDownloader {
  def fileNameFor(url: String, name: String): String = {
    PathUtils.validFileName(URLFilePathParser.withDefaultFileName(url, name).name, "_")
  }

  def fileNameFor(f: FileToDownload): String = fileNameFor(f.url, f.name)

  /**
   * @return Default implementation (wrapped)
   */
  def apply(): WrappedFileDownloader = new HttpClientFileDownloader with WrappedFileDownloader

  def referer[U](url: U)(implicit toUrl: URLProvider[U]): HttpHeader = HttpHeader("Referer", toUrl(url).toString)
}

final case class FileToDownload(url: String, directory: String, name: String = "", httpHeaders: Seq[HttpHeader] = Nil, cookies: Traversable[Cookie] = Nil, sendReport: Boolean = false) {
  override def toString: String = s"FileToDownload($url → $directory [$name])"
}

final case class DownloadedFileReport(url: String, fileName: String, time: DateTime = DateTime.now())

trait LoadedFile extends Closeable {
  def status: HttpStatus

  def contentLength: Option[Long]

  def responseHeaders: Seq[HttpHeader]

  def openStream(): InputStream
}

trait FileDownloader extends Closeable {
  def loadFile(url: String, headers: Seq[HttpHeader], cookies: Traversable[Cookie]): LoadedFile
}

