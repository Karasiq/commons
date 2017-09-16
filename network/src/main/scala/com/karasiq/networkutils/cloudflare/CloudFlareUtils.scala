package com.karasiq.networkutils.cloudflare

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage

import com.karasiq.networkutils.HtmlUnitUtils
import com.karasiq.networkutils.proxy.Proxy

object CloudFlareUtils {
  def compatibleWebClient(js: Boolean = true, cache: Cache = new Cache,
                          cookieManager: CookieManager = new CookieManager, proxy: Option[Proxy] = None): WebClient = {
    val webClient = HtmlUnitUtils.newWebClient(js, redirect = true, ignoreStatusCode = true, cache = cache,
      cookieManager = cookieManager, browserVersion = BrowserVersion.FIREFOX_45, proxy = proxy)
    webClient
  }

  def isCloudFlarePage(page: Page): Boolean = page match {
    case htmlPage: HtmlPage if htmlPage.getWebResponse.getStatusCode == 503 && htmlPage.asXml().contains("Checking your browser") ⇒
      true

    case _ ⇒
      false
  }

  def isCloudFlareCaptchaPage(page: Page): Boolean = page match {
    case htmlPage: HtmlPage if htmlPage.getWebResponse.getStatusCode == 403 && htmlPage.asXml().contains("Please complete the security check to access") ⇒
      true

    case _ ⇒
      false
  }
}
