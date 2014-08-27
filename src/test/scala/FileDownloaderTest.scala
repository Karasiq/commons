import java.nio.file.Files

import com.karasiq.networkutils.downloader.FileDownloader
import org.scalatest.{FlatSpec, Matchers}

// Requires network
class FileDownloaderTest extends FlatSpec with Matchers {
  val downloader = FileDownloader()

  "File downloader" should "download file" in {
    val testFile = Files.createTempFile("fd-test", null)
    downloader.download("http://i-tools.org/random/exec?submit=1&size=4K&download=1", testFile.getParent.toString, testFile.getFileName.toString)
    Files.size(testFile) should be (4096)
    Files.delete(testFile)
  }
}
