package com.karasiq.networkutils.downloader

import java.nio.file.Path

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, SupervisorStrategy}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}


object FileDownloaderActor {
  def apply(): FileDownloaderActor = new HttpClientFileDownloader with FileDownloaderActor
}

trait FileDownloaderActor extends WrappedFileDownloader with Actor with ActorLogging { this: FileDownloader ⇒
  implicit val executionContext = context.system.dispatchers.lookup("fileDownloader.dispatcher")

  final def download(f: FileToDownload): Option[DownloadedFileReport] = download(f.url, f.directory, f.name, f.httpHeaders, f.cookies)

  final def sendReport(report: DownloadedFileReport) = context.sender() ! report

  override def receive = {
    case f: FileToDownload ⇒
      log.debug("Downloading file: {}", f)
      Future(download(f)).onComplete {
        case Failure(exc) ⇒
          log.error(exc, "Error downloading file")
        case Success(report) ⇒
          if (f.sendReport) report.foreach(sendReport)
      }
    case m ⇒
      log.warning("Unknown message: {}", m)
  }

  override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(withinTimeRange = 10 minutes) {
    case _: FileDownloaderException ⇒ Restart
    case _: Exception ⇒ Escalate
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    close()
    super.postStop()
  }

  abstract override protected def onAlreadyDownloaded(path: Path, url: String, loadedFile: LoadedFile): Boolean = {
    log.debug("File already downloaded: {} ({})", path, url)
    super.onAlreadyDownloaded(path, url, loadedFile)
  }

  abstract override protected def onSuccess(report: DownloadedFileReport, file: LoadedFile): Unit = {
    log.info("Downloaded {} ⇒ {}", report.url, report.fileName)
    super.onSuccess(report, file)
  }
}
