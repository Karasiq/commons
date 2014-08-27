package com.karasiq.networkutils.downloader

import java.nio.file.attribute.FileTime
import java.nio.file.{Files, Path, Paths}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.Locale

import com.karasiq.networkutils.HttpClientUtils.HttpClientCookie
import com.karasiq.networkutils.http.HttpStatus
import com.karasiq.networkutils.http.headers.HttpHeader
import com.karasiq.networkutils.http.headers.HttpHeader._

/**
 * Provides modifiers for FileDownloader
 */
object FileDownloaderTraits {
  import com.karasiq.fileutils.PathUtils._

  trait SkipExisting extends WrappedFileDownloader { this: FileDownloader ⇒
    abstract override protected
    def needLoading(url: String, directory: String, name: String, headers: Seq[HttpHeader], cookies: Traversable[HttpClientCookie]): Boolean = {
      val path = Paths.get(directory, FileDownloader.fileNameFor(url, name))
      !path.exists && super.needLoading(url, directory, name, headers, cookies)
    }
  }

  trait CheckSize extends WrappedFileDownloader { this: FileDownloader ⇒
    abstract override protected
    def needSaving(url: String, file: LoadedFile, savePath: Path): Boolean = {
      val contentLengthEqual = savePath.exists && file.contentLength.contains(savePath.fileSize)
      !contentLengthEqual && super.needSaving(url, file, savePath)
    }
  }

  trait CheckModified extends WrappedFileDownloader { this: FileDownloader ⇒
    private val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)

    private def parseHeader(h: String): Instant = {
      ZonedDateTime.parse(h, formatter).toInstant
    }

    private def formatFileTime(ft: FileTime): String = {
      ZonedDateTime.ofInstant(ft.toInstant, ZoneId.systemDefault())
        .withZoneSameInstant(ZoneId.of("UTC"))
        .format(formatter)
    }

    private def fileModifyTime(file: Path): Option[FileTime] = {
      if (file.exists) Some(file.lastModified)
      else None
    }

    /**
     * @param header Last-Modified header
     * @param file File
     * @return
     */
    private def olderThanFile(header: Option[String], file: Path): Boolean = {
      header match {
        case Some(lm) ⇒ fileModifyTime(file) match {
          case Some(ft) ⇒
            val diff = ft.toInstant.compareTo(parseHeader(lm))
            diff < 0
          case _ ⇒ false
        }
        case _ ⇒ false
      }
    }

    private def modified(file: LoadedFile, savePath: Path): Boolean = {
      import org.apache.http.{HttpStatus => HttpStatusCodes}
      file.status match {
        case HttpStatus(HttpStatusCodes.SC_NOT_MODIFIED, _) ⇒
          false

        case HttpStatus(HttpStatusCodes.SC_OK, _) if olderThanFile(file.responseHeaders.get("Last-Modified"), savePath) ⇒
          false

        case _ ⇒
          true
      }
    }

    abstract override def download(url: String, directory: String, name: String, headers: Seq[HttpHeader], cookies: Traversable[HttpClientCookie]): Option[DownloadedFileReport] = {
      val newHeaders = fileModifyTime(Paths.get(directory, FileDownloader.fileNameFor(url, name))).fold(headers)(m ⇒ headers + ("If-Modified-Since" → formatFileTime(m)))
      super.download(url, directory, name, newHeaders, cookies)
    }

    abstract override protected def needSaving(url: String, file: LoadedFile, savePath: Path): Boolean = {
      modified(file, savePath) && super.needSaving(url, file, savePath)
    }

    abstract override protected
    def onSuccess(report: DownloadedFileReport, file: LoadedFile): Unit = {
      file.responseHeaders.get("Last-Modified").foreach { lm ⇒
        // Replace last modified time
        Files.setLastModifiedTime(asPath(report.fileName), FileTime.from(parseHeader(lm)))
      }
      super.onSuccess(report, file)
    }
  }
}
