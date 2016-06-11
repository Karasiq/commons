package com.karasiq.networkutils.cloudflare

import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.karasiq.networkutils.HtmlUnitUtils

object CloudFlareUtils {
  def compatibleWebClient(js: Boolean = true, cache: Cache = new Cache, cookieManager: CookieManager = new CookieManager) = {
    val webClient = HtmlUnitUtils.newWebClient(js, redirect = true, ignoreStatusCode = true, cache = cache, cookieManager = cookieManager, browserVersion = BrowserVersion.FIREFOX_45)
    webClient
  }

  def isCloudFlarePage(page: Page) = page match {
    case htmlPage: HtmlPage if htmlPage.getWebResponse.getStatusCode == 503 && htmlPage.asXml().contains("Checking your browser") ⇒
      true

    case _ ⇒
      false
  }

  def isCloudFlareCaptchaPage(page: Page) = page match {
    case htmlPage: HtmlPage if htmlPage.getWebResponse.getStatusCode == 403 && htmlPage.asXml().contains("Please complete the security check to access") ⇒
      true

    case _ ⇒
      false
  }
}
