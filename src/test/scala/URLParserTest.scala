import com.karasiq.networkutils.url.{URLFilePathParser, URLParser}
import org.scalatest.{FlatSpec, Matchers}

class URLParserTest extends FlatSpec with Matchers {
  "URL parser" should "test URLs for validity" in {
    import URLParser._
    assert(isValidURL("https://www.google.ru/"))
    assert(!isValidURL("not url"))
    assert(isValidURL(withDefaultProtocol("google.ru").toString))
  }

  it should "parse URL" in {
    val parser = URLParser("http://example.com/path/index.php?query=test")

    // Protocol
    parser.protocol should be ("http")
    parser.host should be ("example.com")
    parser.port should be (80)

    // Query
    parser.query should be ("query=test")
    parser.queryParser.toMap should contain ("query" → "test")

    // File path
    parser.file.path should be ("/path/index.php")
    URLFilePathParser.withoutFile(parser.file.path) should be ("/path/")
    parser.file.name should be ("index.php")
    parser.file.extension should be ("php")

    // File path with default name
    val withDefault = URLFilePathParser.withDefaultFileName(parser.toURL, "default.file.name")
    withDefault.path should be ("/path/default.file.name")
    withDefault.name should be ("default.file.name")
    withDefault.extension should be ("name")
  }
}
