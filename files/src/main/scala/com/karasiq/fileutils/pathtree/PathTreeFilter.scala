package com.karasiq.fileutils.pathtree

import java.io.IOException
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, FileVisitor, Path}

import scala.collection.mutable

abstract class PathTreeFilter {
  def needCollect(p: Path): Boolean
  def needTraverse(p: Path): Boolean

  def fileVisitor(buffer: mutable.Builder[Path, _]): FileVisitor[Path] = new FileVisitor[Path] {
    override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE

    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (needCollect(file)) buffer += file
      FileVisitResult.CONTINUE
    }

    override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
      if (needCollect(dir)) buffer += dir
      if (needTraverse(dir)) FileVisitResult.CONTINUE else FileVisitResult.SKIP_SUBTREE
    }

    override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = FileVisitResult.CONTINUE
  }
}

object PathTreeFilter {
  def apply(collectFilter: Path ⇒ Boolean = _ ⇒ true, traverseFilter: Path ⇒ Boolean = _ ⇒ true): PathTreeFilter = new PathTreeFilter {
    override def needCollect(p: Path): Boolean = collectFilter(p)

    override def needTraverse(p: Path): Boolean = traverseFilter(p)
  }
}

