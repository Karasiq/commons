package com.karasiq.networkutils.downloader

import java.io.IOException

import scala.util.control.Exception

/**
 * General file downloader exception
 */
class FileDownloaderException(message: String = null, cause: Throwable = null) extends IOException(message, cause)

object FileDownloaderException {
  def apply(s: String, e: Throwable): FileDownloaderException = new FileDownloaderException(s"Error downloading file: $s", e)

  def apply(e: Throwable): FileDownloaderException = new FileDownloaderException("Error downloading file", e)

  def apply(s: String): FileDownloaderException = new FileDownloaderException(s"Error downloading file: $s")

  def wrap[T]: Exception.Catch[T] = {
    Exception.catching(classOf[IOException])
      .withApply(e ⇒ throw this.apply("Error downloading file", e))
  }
}