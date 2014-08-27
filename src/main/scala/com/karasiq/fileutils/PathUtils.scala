package com.karasiq.fileutils


import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import org.apache.commons.io.{FileUtils, FilenameUtils, IOUtils}

import scala.collection.GenSeq
import scala.collection.JavaConversions._

object RootedPathSeq {
  import PathUtils._
  def apply[T](s: T, nodes: Seq[String] = Seq.empty)(implicit toPath: PathProvider[T]): RootedPathSeq = new RootedPathSeq(toPath(s), nodes)
}

case class RootedPathSeq(root: Path, nodes: Seq[String]) {
  assert(nodes.forall(_.nonEmpty), "Invalid path")

  private def separator: String = IOUtils.DIR_SEPARATOR.toString

  def toPath: Path = Paths.get(root.toAbsolutePath.toString, nodes:_*)
  def toPathString: String = toPath.toString
  def toRelativePathString: String = nodes.mkString(separator, separator, separator)
  def apply(i: Int): String = nodes.apply(i)
  def withRoot(r: Path): RootedPathSeq = copy(root = r)
  def withNode(i: Int, n: String): RootedPathSeq = copy(nodes = this.nodes.updated(i, n))
  def :/(ns: Seq[String]): RootedPathSeq = if (ns.forall(_.nonEmpty)) copy(nodes = this.nodes ++ ns) else this
  def /(n: String): RootedPathSeq = :/(Seq(n))
  def nodeCount: Int = nodes.length
  def asRoot: RootedPathSeq = RootedPathSeq(PathUtils.Conversions.rootedPathSeqToPath(this), Seq.empty)

  override def toString = {
    toPathString
    // if (nodes.nonEmpty) s"RootedPathSeq($root, $toRelativePathString)" else s"RootedPathSeq($root)"
  }
}

object PathUtils {
  object Conversions {
    import scala.language.implicitConversions

    implicit def rootedPathSeqToPath(rps: RootedPathSeq): Path = rps.toPath
    implicit def stringToPath(s: String): Path = Paths.get(s)
    implicit def stringSeqToRootedPathSeq(ss: Seq[String]): RootedPathSeq = RootedPathSeq(ss.head, ss.tail)(StringPathProvider)
    implicit def stringSeqToPath(ss: Seq[String]): Path = Paths.get(ss.head, ss.tail:_*)
    implicit def pathToRootedPathSeq(p: Path): RootedPathSeq = RootedPathSeq(p)(PathPathProvider)
  }

  trait PathProvider[T] {
    def apply(src: T): Path
  }

  implicit object PathPathProvider extends PathProvider[Path] {
    override def apply(src: Path): Path = src // Just return object
  }

  implicit object StringPathProvider extends PathProvider[String] {
    override def apply(src: String): Path = Conversions.stringToPath(src)
  }

  implicit object StringSeqPathProvider extends PathProvider[Seq[String]] {
    override def apply(src: Seq[String]): Path = Conversions.stringSeqToPath(src)
  }

  implicit object RootedPathSeqPathProvider extends PathProvider[RootedPathSeq] {
    override def apply(src: RootedPathSeq): Path = Conversions.rootedPathSeqToPath(src)
  }

  implicit object FilePathProvider extends PathProvider[File] {
    override def apply(src: File): Path = src.toPath
  }

  def asPath[T](src: T)(implicit pp: PathProvider[T]): Path = {
    pp(src)
  }

  def validFileName(s: String, r: String = ""): String = s.replaceAll("[\\\\/\\?\"\\*:<>\\|]+", r).trim

  def getOrCreate(f: Path): Path = if (f.exists) f else {
    Files.createDirectories(f.getParent)
    Files.createFile(f)
  }

  def getOrCreate(s: String): Path = getOrCreate(asPath(s))

  implicit class PathGenericOps(path: Path) {
    def isDirectory: Boolean = Files.isDirectory(path)
    def isRegularFile: Boolean = Files.isRegularFile(path)
    def isSymbolicLink: Boolean = Files.isSymbolicLink(path)
    def exists: Boolean = Files.exists(path)

    def name: String = path.getFileName.toString
    def extension: String = FilenameUtils.getExtension(name)
    def baseName: String = FilenameUtils.getBaseName(name)
    def pathTree: Iterator[Path] = path.iterator()

    /**
     * Size of the file
     */
    def fileSize: Long = Files.size(path)

    /**
     * Last modified time
     */
    def lastModified = Files.getLastModifiedTime(path)

    /**
     * Attributes of specified type
     * @tparam A Attributes type
     */
    def attributesOf[A <: BasicFileAttributes](implicit m: Manifest[A]): A = Files.readAttributes(path, m.runtimeClass.asInstanceOf[Class[A]])

    /**
     * Basic attributes
     */
    def attributes: BasicFileAttributes = attributesOf[BasicFileAttributes]

    // Streams
    import java.io.{BufferedInputStream, BufferedOutputStream, FileInputStream, FileOutputStream}
    def inputStream(): FileInputStream = new FileInputStream(path.toFile)
    def bufferedInputStream(): BufferedInputStream = new BufferedInputStream(inputStream())
    def outputStream(): FileOutputStream = new FileOutputStream(path.toFile)
    def bufferedOutputStream(): BufferedOutputStream = new BufferedOutputStream(outputStream())

    def relative(s: String): Path = path.resolve(s)
    def relative(p: Path): Path = path.resolve(p)

    def relativePathTree(from: Path): RootedPathSeq = {
      RootedPathSeq(from, path.pathTree.toIndexedSeq.diff(from.pathTree.toIndexedSeq).namesSeq.toIndexedSeq)
    }

    /**
     * Deletes file or link
     */
    def deleteFile(): Unit = Files.delete(path)

    /**
     * Deletes non-empty directory
     */
    def deleteDir(): Unit = FileUtils.deleteDirectory(path.toFile)

    /**
     * Deletes file or directory
     */
    def deleteFileOrDir(): Unit = if (path.isDirectory && !path.isSymbolicLink) deleteDir() else deleteFile()
  }

  implicit class PathChecksumOps(file: Path) {
    import java.util.zip.{Adler32, CRC32, CheckedInputStream, Checksum}
    assert(file.isRegularFile, s"Invalid file: $file")

    private def checksum(cs: Checksum): Long = {
      val inputStream = file.inputStream()
      try {
        val cis = new CheckedInputStream(inputStream, cs)
        val tempBuf = new Array[Byte](1024)
        cis.read()
        while(cis.read(tempBuf) >= 0) { /* Do nothing */ }
        cis.getChecksum.getValue
      } finally {
        IOUtils.closeQuietly(inputStream)
      }
    }

    lazy val crc32 = checksum(new CRC32)
    lazy val adler32 = checksum(new Adler32)

    def hashesEquals(f: Path): Boolean = f.crc32 == crc32 && f.adler32 == adler32
  }

  implicit class PathSeqOps(p: GenSeq[Path]) {
    def namesSeq: GenSeq[String] = p.map(_.name)
  }
}
