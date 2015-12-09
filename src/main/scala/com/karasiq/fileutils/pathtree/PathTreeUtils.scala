package com.karasiq.fileutils.pathtree

import java.io.Closeable
import java.nio.file.{DirectoryStream, FileVisitOption, Files, Path}
import java.util.concurrent.atomic.AtomicBoolean

import com.karasiq.fileutils.PathUtils._
import org.apache.commons.io.IOUtils

import scala.collection.AbstractIterator
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

object PathTreeUtils {
  val defaultTreeFilter: PathTreeFilter = PathTreeFilter()

  private final class DirectoryStreamIterator[T](ds: DirectoryStream[T]) extends AbstractIterator[T] with Closeable {
    private val underlying = ds.iterator()

    private val closed = new AtomicBoolean(false)

    override def next(): T = {
      if (!closed.get() && underlying.hasNext) {
        val next = underlying.next()
        if (!underlying.hasNext) this.close() // Last element
        next
      } else {
        Iterator.empty.next()
      }
    }

    override def hasNext: Boolean = {
      underlying.hasNext
    }

    def close(): Unit = {
      if (closed.compareAndSet(false, true)) {
        ds.close()
      }
    }

    override def finalize(): Unit = {
      IOUtils.closeQuietly(this)
      super.finalize()
    }
  }

  private implicit def directoryStreamToIterator[T](ds: DirectoryStream[T]): Iterator[T] = new DirectoryStreamIterator(ds)

  implicit class PathTraverseOps(val dir: Path) extends AnyVal {
    def subFilesAndDirs: Iterator[Path] = if (dir.isDirectory) Files.newDirectoryStream(dir) else Iterator.empty

    def subFiles: Iterator[Path] = subFilesAndDirs.filter(_.isRegularFile)

    def subDirs: Iterator[Path] = subFilesAndDirs.filter(_.isDirectory)

    def nextLevel(treeFilter: PathTreeFilter = defaultTreeFilter): Iterator[Path] =
      subDirs.filter(treeFilter.needTraverse).flatMap(_.subFilesAndDirs).filter(treeFilter.needCollect)

    def traverse(depth: Int, treeFilter: PathTreeFilter = defaultTreeFilter, followLinks: Boolean = false): Seq[Path] = {
      val buffer = new ListBuffer[Path]()
      val options = setAsJavaSet[FileVisitOption](if (followLinks) Set(FileVisitOption.FOLLOW_LINKS) else Set.empty)
      Files.walkFileTree(dir, options, depth, treeFilter.fileVisitor(buffer))
      buffer.result()
    }

    def fullTraverse(treeFilter: PathTreeFilter = defaultTreeFilter, followLinks: Boolean = false): Seq[Path] = {
      this.traverse(Int.MaxValue, treeFilter, followLinks)
    }

    def fullTraverseForSymLinks(): Seq[Path] = fullTraverse(PathTreeFilter(collectFilter = _.isSymbolicLink))

    def fullTraverseForDirectories(): Seq[Path] = fullTraverse(PathTreeFilter(collectFilter = _.isDirectory))

    def fullTraverseForFiles(): Seq[Path] = fullTraverse(PathTreeFilter(collectFilter = _.isRegularFile))
  }

  implicit class PathTraversedOps(val files: Traversable[Path]) extends AnyVal {
    def onlyDirs = files.filter(_.isDirectory)
    def onlyFiles = files.filter(_.isRegularFile)
	  def onlySymLinks = files.filter(_.isSymbolicLink)
    def subFilesAndDirs = files.flatMap(_.subFilesAndDirs)
    def subFiles = files.flatMap(_.subFiles)
    def subDirs = files.flatMap(_.subDirs)

    def findDuplicatesBy[T](func: Path ⇒ T): FileDuplicatesOps[T] = new FileDuplicatesOps({
      val seq = files.toSeq.zip(files.toSeq.map(func))
      seq.filter(f ⇒ seq.count(_._2 == f._2) > 1)
        .groupBy(_._2)
        .mapValues(_.map(_._1).toSet)
    })

    def findDuplicatesByHash(): FileDuplicatesOps[Long] = onlyFiles.findDuplicatesBy(_.crc32)

    def findDuplicatesByName(): FileDuplicatesOps[String] = findDuplicatesBy(_.name)

    def findDuplicatesBySize(): FileDuplicatesOps[Long] = onlyFiles.findDuplicatesBy(_.fileSize)
  }

  class FileDuplicatesOps[T](val duplicates: Map[T, Set[Path]]) extends AnyVal {
    def deleteBy(f: (T, Set[Path]) ⇒ Path) {
      duplicates.flatMap(d ⇒ d._2 - f.tupled(d))
        .foreach(_.deleteFileOrDir())
    }
  }
}
