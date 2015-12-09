package com.karasiq.fileutils.watcher

import java.nio.file.StandardWatchEventKinds._
import java.nio.file.{Path, StandardWatchEventKinds, WatchEvent}

import akka.actor.Actor

abstract class FileWatcherServiceWrapper(eventBus: FileWatcherService.FsEventBus, events: Set[WatchEvent.Kind[Path]] = Set(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY)) extends Actor {
  def onModify(event: WatchedFileEvent): Unit = ()

  def onDelete(event: WatchedFileEvent): Unit = ()

  def onCreate(event: WatchedFileEvent): Unit = ()

  override final def receive: Receive = {
    case ev: WatchedFileEvent ⇒
      ev.kind match {
        case ENTRY_MODIFY ⇒
          onModify(ev)
        case ENTRY_DELETE ⇒
          onDelete(ev)
        case ENTRY_CREATE ⇒
          onCreate(ev)
      }
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    eventBus.unsubscribe(self)
    super.postStop()
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    eventBus.subscribe(self, events)
    super.preStart()
  }
}
