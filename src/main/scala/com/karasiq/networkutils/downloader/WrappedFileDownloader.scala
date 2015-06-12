package com.karasiq.networkutils.downloader

import java.nio.file.{Files, Path, Paths}

import com.karasiq.common.ThreadLocalFactory
import com.karasiq.fileutils.PathUtils._
import com.karasiq.networkutils.http.headers.HttpHeader
import org.apache.commons.io.IOUtils
import org.apache.http.cookie.Cookie

trait WrappedFileDownloader { this: FileDownloader ⇒
  protected def onAlreadyDownloaded(path: Path, url: String, loadedFile: LoadedFile): Boolean = false

  protected def onSuccess(report: DownloadedFileReport, file: LoadedFile): Unit = ()

  protected def needLoading(url: String, directory: String, name: String, headers: Seq[HttpHeader], cookies: Traversable[Cookie]): Boolean = true

  protected def needSaving(url: String, file: LoadedFile, savePath: Path): Boolean = {
    if (!file.status.isOk) throw FileDownloaderException(s"$url (${file.status.code} ${file.status.message})")
    true
  }

  // File download buffer
  private val buffer = ThreadLocalFactory.weakRef(new Array[Byte](524288))

  private def copyToFile(loadedFile: LoadedFile, path: Path): Unit = {
    import IOUtils.{closeQuietly, copyLarge}
    val inputStream = loadedFile.openStream()
    val outputStream = path.outputStream()

    try {
      copyLarge(inputStream, outputStream, buffer())
    } finally {
      closeQuietly(inputStream)
      closeQuietly(outputStream)
    }
  }

  @throws[FileDownloaderException]("If downloading error occurs")
  def download(url: String, directory: String, name: String = "", headers: Seq[HttpHeader] = Nil, cookies: Traversable[Cookie] = Nil): Option[DownloadedFileReport] = FileDownloaderException.wrap(url) {
    var result: Option[DownloadedFileReport] = None

    if (needLoading(url, directory, name, headers, cookies)) {
      val loadedFile: LoadedFile = loadFile(url, headers, cookies)
      try {
        val filePath = Paths.get(directory, FileDownloader.fileNameFor(url, name))
        if (this.needSaving(url, loadedFile, filePath) || this.onAlreadyDownloaded(filePath, url, loadedFile)) {
          Files.createDirectories(filePath.getParent)
          val report = DownloadedFileReport(url, filePath.toAbsolutePath.toString)
          copyToFile(loadedFile, filePath)
          this.onSuccess(report, loadedFile)
          result = Some(report)
        }
      } finally {
        IOUtils.closeQuietly(loadedFile)
      }
    }

    result
  }
}
