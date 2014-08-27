package com.karasiq.networkutils

import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._
import com.karasiq.common.StringUtils
import com.karasiq.networkutils.proxy.Proxy
import com.karasiq.networkutils.url.URLProvider

import scala.collection.GenTraversableOnce
import scala.collection.JavaConversions._
import scala.util.control

/**
 * Dynamic HTML element traverse helper
 * @param element HTML element
 */
case class HtmlTraverseOps[T <: HtmlElement](element: Option[T]) {
  import com.karasiq.networkutils.HtmlUnitUtils._

  /**
   * First by predicate
   * @param f Predicate function
   * @return First matched sub-element
   */
  def \(f: HtmlElement ⇒ Boolean): HtmlTraverseOps[HtmlElement] = HtmlTraverseOps(element.flatMap(_.subElements.find(f)))

  /**
   * All by predicate
   * @param f Predicate function
   * @return All matched sub-elements
   */
  def \\(f: HtmlElement ⇒ Boolean): Iterator[HtmlElement] = element.toIterator.flatMap(_.subElements.filter(f))

  /**
   * First by class
   * @tparam T1 HtmlElement subclass
   * @return First sub-element with specified class
   */
  def \[T1 <: HtmlElement](c: Class[T1])(implicit m: Manifest[T1]): HtmlTraverseOps[T1] = HtmlTraverseOps(\\(c).toIterable.headOption)

  /**
   * All by class
   * @tparam T1 HtmlElement subclass
   * @return All sub-elements with specified class
   */
  def \\[T1 <: HtmlElement](c: Class[T1])(implicit m: Manifest[T1]): Iterator[T1] = element.toIterator.flatMap(_.subElementsOf[T1])

  /**
   * First by HTML class
   * @param c HTML class attribute
   * @return First sub-element that has a `c` HTML class
   */
  def @\(c: String): HtmlTraverseOps[HtmlElement] = \(_.classes.contains(c))

  /**
   * All by HTML class
   * @param c HTML class attribute
   * @return Sub-elements that has a `c` HTML class
   */
  def @\\(c: String): Iterator[HtmlElement] = \\(_.classes.contains(c))
}

/**
 * Dynamic HTML element filter helper
 * @param elements Set of HTML elements
 * @tparam T Type of HTML elements
 */
case class HtmlFilterOps[T <: HtmlElement](elements: GenTraversableOnce[T]) {
  import com.karasiq.networkutils.HtmlUnitUtils._

  /**
   * Find by predicate
   * @param f Predicate function
   * @return First matched element
   */
  def *\(f: T => Boolean): Option[T] = elements.find(f)

  /**
   * Filter by predicate
   * @param f Predicate function
   * @return HtmlFilterOps scope of matched elements
   */
  def *\\(f: T => Boolean): HtmlFilterOps[T] = HtmlFilterOps[T](elements.toIterable.filter(f))

  /**
   * First by class
   * @tparam T1 HtmlElement subclass
   * @return First element with specified class
   */
  def *\[T1 <: HtmlElement](c: Class[T1])(implicit m: Manifest[T1]): Option[T1] = elements.toIterable.collect{case e: T1 => e}.headOption

  /**
   * Filter by class
   * @tparam T1 HtmlElement subclass
   * @return HtmlFilterOps scope of elements with specified class
   */
  def *\\[T1 <: HtmlElement](c: Class[T1])(implicit m: Manifest[T1]): HtmlFilterOps[T1] = HtmlFilterOps[T1](elements.toIterable.collect { case e: T1 => e })

  /**
   * Finds first by class HTML attribute
   * @param c HTML class
   * @return First element with specified html class attribute
   */
  def *@\(c: String) = *\(_.classes.contains(c))

  /**
   * Filter by class HTML attribute
   * @param c HTML class
   * @return HtmlFilterOps scope of elements with specified html class attribute
   */
  def *@\\(c: String) = *\\(_.classes.contains(c))

  /**
   * Get by index
   * @param n Element index
   * @return Some(element) or None if not found
   */
  def #\(n: Int): Option[HtmlElement] = {
    val seq = elements.toIndexedSeq; if (seq.length <= n) None else Some(seq(n))
  }
}

object HtmlUnitUtils {
  import scala.language.implicitConversions

  private val loggingDisabled = new AtomicBoolean(false)

  /**
   * Disables HtmlUnit logging
   */
  def disableLogging(): Unit = {
    if (loggingDisabled.compareAndSet(false, true)) {
      import java.util.logging.Level

      import org.apache.commons.logging.LogFactory
      LogFactory.getFactory.setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
      java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF)
      java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF)
    }
  }

  class PageURLProvider[P <: Page] extends URLProvider[P] {
    override def apply(src: P): URL = src.getUrl
  }

  implicit def pageUrlProvider[P <: Page]: PageURLProvider[P] = new PageURLProvider[P]

  class HtmlElementURLProvider[E <: HtmlElement] extends URLProvider[E] {
    import url._
    override def apply(src: E): URL = src match {
      case img: HtmlImage ⇒
        asURL(img.fullSrc)

      case a: HtmlAnchor ⇒
        asURL(a.fullHref)

      case a: HtmlLink ⇒
        asURL(a.fullUrl(_.getHrefAttribute))

      case a: HtmlScript ⇒
        asURL(a.fullUrl(_.getSrcAttribute))

      case _ ⇒
        throw new IllegalArgumentException("HTML element does not contain URL")
    }
  }

  implicit def htmlElementUrlProvider[E <: HtmlElement]: HtmlElementURLProvider[E] = new HtmlElementURLProvider[E]

  implicit def elementOptionTraverseOps[T <: HtmlElement](opt: Option[T]): HtmlTraverseOps[T] = HtmlTraverseOps[T](opt)

  implicit def elementTraverseOps[T <: HtmlElement](e: T): HtmlTraverseOps[T] = elementOptionTraverseOps[T](Some(e))

  implicit def elementsFilterOps[T <: HtmlElement](e: GenTraversableOnce[T]): HtmlFilterOps[T] = HtmlFilterOps[T](e)

  implicit def htmlTraverseOpsGetElement[T <: HtmlElement](ops: HtmlTraverseOps[T]): Option[T] = ops.element

  implicit def htmlFilterOpsGetElements[T <: HtmlElement](ops: HtmlFilterOps[T]): GenTraversableOnce[T] = ops.elements

  type HtmlUnitCookie = com.gargoylesoftware.htmlunit.util.Cookie

  /**
   * Creates new WebClient with specified parameters
   * @param js Enable JavaScript
   * @param redirect Enable HTTP redirect
   * @param ignoreStatusCode Not throw exceptions on erroneous HTTP status code
   * @param browserVersion Emulated browser type
   * @param cache Cache storage
   * @param cookieManager Cookie storage
   * @param ajaxController AJAX handler
   * @return
   */
  def newWebClient(js: Boolean = true, redirect: Boolean = true, ignoreStatusCode: Boolean = true, browserVersion: BrowserVersion = BrowserVersion.getDefault, cache: Cache = new Cache, cookieManager: CookieManager = new CookieManager, ajaxController: AjaxController = new NicelyResynchronizingAjaxController, proxy: Option[Proxy] = Proxy.default): WebClient = {
    val webClient = new WebClient(browserVersion)

    webClient.setCache(cache)
    webClient.setCookieManager(cookieManager)
    proxy.foreach(p ⇒ webClient.getOptions.setProxyConfig(p))

    webClient.getOptions.setThrowExceptionOnFailingStatusCode(!ignoreStatusCode)
    webClient.getOptions.setCssEnabled(false)

    webClient.getOptions.setJavaScriptEnabled(js)
    if (js) {
      webClient.getOptions.setThrowExceptionOnScriptError(false)
      webClient.setAjaxController(ajaxController)
    }

    webClient.getOptions.setTimeout(30000)
    webClient.getOptions.setRedirectEnabled(redirect)
    webClient.getOptions.setPopupBlockerEnabled(true)
    webClient
  }

  implicit class PageOps(page: Page) {
    def responseHeader(name: String): Option[String] = {
      Option(page.getWebResponse.getResponseHeaderValue(name))
    }
  }

  implicit class HtmlPageContentOps(page: HtmlPage) {
    def elementOption[E <: HtmlElement](f: HtmlPage ⇒ E): Option[E] =
      scala.util.control.Exception.catching(classOf[ElementNotFoundException], classOf[NullPointerException])
        .opt(f(page)).filter(_ != null)

    def elementsBy[E <: HtmlElement](f: HtmlPage ⇒ GenTraversableOnce[E]): Iterator[E] =
      scala.util.control.Exception.catching(classOf[ElementNotFoundException], classOf[NullPointerException]).opt(f(page)) match {
        case None | Some(null) ⇒
          Iterator.empty
        case Some(elements) ⇒
          elements.toIterator
      }

    def elementsByTagName[E <: HtmlElement](tag: String)(implicit m: Manifest[E]): Iterator[E] = page.getDocumentElement.subElementsByTagName[E](tag)

    def descendantsBy[R](pf: PartialFunction[HtmlElement, R]): Iterator[R] = page.getDocumentElement.subElementsBy(pf)

    def descendantsOf[E <: HtmlElement](implicit m: Manifest[E]) = page.getDocumentElement.subElementsOf[E]

    def anchors: Iterator[HtmlAnchor] = descendantsOf[HtmlAnchor]

    def images: Iterator[HtmlImage] = descendantsOf[HtmlImage]

    def byXPath[T <: HtmlElement](xpath: String)(implicit m: Manifest[T]): Iterator[T] = {
      assert(xpath.nonEmpty, "Invalid XPath")
      page.elementsBy[T](_.getByXPath(xpath).toIterator.collect { case e: T ⇒ e })
    }

    def firstByXPath[T <: HtmlElement](xpath: String)(implicit m: Manifest[T]): Option[T] = {
      assert(xpath.nonEmpty, "Invalid XPath")
      page.getFirstByXPath[T](xpath) match {
        case e: T ⇒
          Some(e)
        case _ ⇒
          None
      }
    }
  }

  implicit class WebClientOps(webClient: WebClient) {
    def closeAllWindowsAfter[T](f: ⇒ T): T = {
      control.Exception.allCatch.andFinally(webClient.closeAllWindows())(f)
    }

    def withGetPage[T <: Page, R, U](url: U)(f: T ⇒ R)(implicit toUrl: URLProvider[U]): R = {
      val page = webClient.getPage[T](toUrl(url))
      control.Exception.allCatch.andFinally(page.cleanUp())(f(page))
    }

    def withGetHtmlPage[R, U](url: U)(f: HtmlPage ⇒ R)(implicit toUrl: URLProvider[U]): R = {
      withGetPage[HtmlPage, R, U](url)(f)
    }

    def withCookies[T](cookies: CookieManager)(f: ⇒ T): T = {
      val oldCookies = webClient.getCookieManager
      webClient.setCookieManager(cookies)
      control.Exception.allCatch
        .andFinally(webClient.setCookieManager(oldCookies))(f)
    }

    def withJavaScript[T](enabled: Boolean = true)(f: WebClient ⇒ T) = {
      val js = webClient.getOptions.isJavaScriptEnabled
      webClient.getOptions.setJavaScriptEnabled(enabled)
      val result: T = f(webClient)
      webClient.getOptions.setJavaScriptEnabled(js)
      result
    }

    def withConfirmAllHandler[T](f: WebClient ⇒ T): T = {
      val ch = webClient.getConfirmHandler
      webClient.setConfirmHandler(new ConfirmHandler {
        override def handleConfirm(page: Page, message: String): Boolean = true
      })
      val result: T = f(webClient)
      webClient.setConfirmHandler(ch)
      result
    }

    def pageOption[T <: Page, U](url: U)(implicit m: Manifest[T], toUrl: URLProvider[U]): Option[T] =
      control.Exception.catching(classOf[FailingHttpStatusCodeException])
        .opt(webClient.getPage[Page](toUrl(url)))
        .collect { case t: T ⇒ t }

    def htmlPageOption[U](url: U)(implicit toUrl: URLProvider[U]): Option[HtmlPage] = {
      pageOption[HtmlPage, U](url)
    }

    def cookies: Set[HtmlUnitCookie] = {
      webClient.getCookieManager.getCookies.toSet
    }

    def cookies[U](url: U)(implicit toUrl: URLProvider[U]): Set[HtmlUnitCookie] = {
      webClient.getCookies(toUrl(url)).toSet
    }

    def addCookies(cookies: GenTraversableOnce[HtmlUnitCookie]) {
      cookies.foreach(webClient.getCookieManager.addCookie)
    }

    def setCookies(cookies: GenTraversableOnce[HtmlUnitCookie]) {
      webClient.getCookieManager.clearCookies()
      addCookies(cookies)
    }
  }

  implicit class HtmlElementOps[E <: HtmlElement](e: E) {
    def htmlPage: HtmlPage = {
      Option(e.getHtmlPageOrNull)
        .getOrElse(throw new IllegalArgumentException("Couldn't find HTML page"))
    }

    def webClient: WebClient = this.htmlPage.getWebClient

    def fullUrl(f: E ⇒ String): String = {
      val attr = f(e)
      Option(e.getHtmlPageOrNull).fold(attr)(_.getFullyQualifiedUrl(attr).toString)
    }

    def subElements: Iterator[HtmlElement] = e.getHtmlElementDescendants.toIterator

    def subElementsBy[R](pf: PartialFunction[HtmlElement, R]): Iterator[R] = subElements.collect(pf)

    def subElementsByTagName[R <: HtmlElement](tag: String)(implicit m: Manifest[R]): Iterator[R] = subElementsBy {
      case e: R if e.getTagName == tag ⇒
        e
    }

    def subElementsOf[T <: HtmlElement](implicit m: Manifest[T]): Iterator[T] = subElementsBy { case e: T ⇒ e }

    def classAttribute: String = e.getAttribute("class")

    def classes: Set[String] = classAttribute.split(' ').map(StringUtils.htmlTrim).filter(_.nonEmpty).toSet

    def tryClick: Option[HtmlPage] = {
      control.Exception.catching(classOf[FailingHttpStatusCodeException], classOf[ScriptException]).opt(e.click[HtmlPage]())
    }
  }

  implicit class HtmlAnchorOps(e: HtmlAnchor) extends HtmlElementOps(e) {
    def fullHref: String = fullUrl(_.getHrefAttribute)
  }

  implicit class HtmlImageOps(e: HtmlImage) extends HtmlElementOps(e) {
    def fullSrc: String = fullUrl(_.getSrcAttribute)
  }

  implicit class HtmlUnitCookiesOps(cookies: Traversable[HtmlUnitCookie]) {
    def toHttpClient = {
      import com.gargoylesoftware.htmlunit.util.Cookie.{toHttpClient => convert}
      convert(asJavaCollection(cookies.toIterable)).toTraversable
    }
  }

  implicit def htmlElementOptionClick[A <: HtmlElement](e: Option[A]): Option[HtmlPage] = e.flatMap(_.tryClick)

  implicit def proxyToProxyConfig(p: Proxy): ProxyConfig = {
    import p._
    new ProxyConfig(host, port, scheme == "socks")
  }

  implicit def proxyConfigToProxy(pc: ProxyConfig): Proxy = new Proxy {
    override def host: String = pc.getProxyHost

    override def scheme: String = if (pc.isSocksProxy) "socks" else "http"

    override def port: Int = pc.getProxyPort

    override def userInfo: Option[String] = None
  }
}
