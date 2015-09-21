package com.karasiq.networkutils.cloudflare

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.{HtmlElement, HtmlPage}
import com.karasiq.networkutils.HtmlUnitUtils

object CloudFlareUtils {
  def compatibleWebClient(js: Boolean = true, cache: Cache = new Cache, cookieManager: CookieManager = new CookieManager) = {
    val webClient = HtmlUnitUtils.newWebClient(js, redirect = true, ignoreStatusCode = true, cache = cache, cookieManager = cookieManager, browserVersion = BrowserVersion.FIREFOX_38)
    webClient.setScriptPreProcessor(scriptPreprocessor)
    webClient
  }

  def isCloudFlarePage(page: Page) = page match {
    case htmlPage: HtmlPage if htmlPage.getWebResponse.getStatusCode == 503 && htmlPage.asXml().contains("Checking your browser") ⇒
      true

    case _ ⇒
      false
  }


  protected[cloudflare] def scriptPreprocessor: ScriptPreProcessor = new ScriptPreProcessor {
    @inline
    private def noWaitScript(js: String) = {
      if (js.contains("getElementById('cf-content')"))
        js.replaceFirst("}, 5850\\);", "}, 0);")
      else
        js
    }

    override def preProcess(p1: HtmlPage, p2: String, p3: String, p4: Int, p5: HtmlElement): String = noWaitScript(p2)
  }
}
