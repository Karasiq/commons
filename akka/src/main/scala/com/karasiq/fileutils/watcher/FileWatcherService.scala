package com.karasiq.fileutils.watcher

import java.nio.file.StandardWatchEventKinds._
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import akka.actor.SupervisorStrategy.Resume
import akka.actor._
import akka.event.{EventBus, ScanningClassification}
import com.karasiq.fileutils.PathUtils._
import com.karasiq.fileutils.watcher.FileWatcherService.FsEventBus

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

final case class RegisterFile(path: String, recursive: Boolean = false)
final case class UnregisterFile(path: String, recursive: Boolean = false)
final case class WatchedFileEvent(path: String, file: String, kind: WatchEvent.Kind[Path]) {
  def absolutePath: Path = Paths.get(path, file)
}

object FileWatcherService {
  private[watcher] def watchedFileEvent(key: WatchKey, event: WatchEvent[_]): WatchedFileEvent = {
    val dir = key.watchable() match {
      case p: Path ⇒ p.toString
    }

    val relative = event.context() match {
      case p: Path ⇒ p.toString
    }

    WatchedFileEvent(dir, relative, event.kind().asInstanceOf[WatchEvent.Kind[Path]])
  }

  trait Filter extends FileWatcherService {
    def eventsPreProcess(events: Iterator[WatchedFileEvent]): Iterator[WatchedFileEvent] = events

    abstract override protected def processEvents(events: Iterator[WatchedFileEvent]): Unit = {
      super.processEvents(eventsPreProcess(events))
    }
  }

  class FsEventBus extends EventBus with ScanningClassification {
    override type Event = WatchedFileEvent
    override type Classifier = Set[WatchEvent.Kind[Path]]
    override type Subscriber = ActorRef

    override protected def compareClassifiers(a: Classifier, b: Classifier): Int = {
      if (a == b) 0 else 1
    }

    override protected def matches(classifier: Classifier, event: Event): Boolean = {
      classifier.contains(event.kind)
    }

    override protected def publish(event: Event, subscriber: Subscriber): Unit = {
      subscriber ! event
    }

    override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = {
      a.compareTo(b)
    }
  }
}

class FileWatcherService(eventBus: FsEventBus) extends Actor with ActorLogging {
  import context.dispatcher

  private val watchService = FileSystems.getDefault.newWatchService()
  private val keys = mutable.Map.empty[String, WatchKey]

  protected def pollInterval(): FiniteDuration = 3 seconds

  protected def processEvents(events: Iterator[WatchedFileEvent]): Unit = {
    events.foreach { event ⇒
      log.debug("File event: {}", event)
      eventBus.publish(event)
    }
  }

  private val pollTask = context.system.scheduler.schedule(1 second, pollInterval()) {
    val signaled = Iterator.continually(watchService.poll()).takeWhile(null ne)
    signaled.foreach { key ⇒
      val events = key.pollEvents().iterator().filter(OVERFLOW !=).map { event ⇒ FileWatcherService.watchedFileEvent(key, event) }
      processEvents(events)
      if(!key.reset()) log.debug("Key not valid anymore: {}", key)
    }
  }

  def addFile(path: Path): Unit = {
    val pathString = path.toString

    if (!keys.contains(pathString))
      keys += pathString → path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
  }

  // Recursive
  def addDirectory(path: Path): Unit = {
    if (path.isDirectory) Files.walkFileTree(path, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        addFile(dir)
        FileVisitResult.CONTINUE
      }
    }) else addFile(path)
  }

  def removeFile(path: Path): Unit = {
    val pathString = path.toString

    if (!keys.contains(pathString)) {
      keys.get(pathString).filter(_.isValid).foreach(_.cancel())
      keys -= pathString
    }
  }

  // Recursive
  def removeDirectory(path: Path): Unit = {
    if (path.isDirectory) Files.walkFileTree(path, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        removeFile(dir)
        FileVisitResult.CONTINUE
      }
    }) else removeFile(path)
  }

  override final def receive: Receive = {
    case RegisterFile(path, recursive) ⇒
      val file = Paths.get(path).normalize()
      if (recursive) addDirectory(file)
      else addFile(file)
      log.debug("Key registered: {}", path)
    case UnregisterFile(path, recursive) ⇒
      val file = Paths.get(path).normalize()
      if (recursive) removeDirectory(file)
      else removeFile(file)
      log.debug("Key unregistered: {}", path)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    pollTask.cancel()
    watchService.close()
    super.postStop()
  }

  override def supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
    case e: FileSystemException ⇒ Resume
  }
}