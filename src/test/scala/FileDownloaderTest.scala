import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.tags.Network

import com.karasiq.networkutils.downloader.FileDownloader

@Network
class FileDownloaderTest extends FlatSpec with Matchers {
  val downloader = FileDownloader()

  "File downloader" should "download file" in {
    val testFile = Files.createTempFile("fd-test", null)
    downloader.download("https://example.com/", testFile.getParent.toString, testFile.getFileName.toString)
    Files.size(testFile) shouldBe 1270
    Files.delete(testFile)
  }
}
