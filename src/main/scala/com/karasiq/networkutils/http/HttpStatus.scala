package com.karasiq.networkutils.http
import org.apache.http.{HttpStatus => HttpStatusCodes}

final case class HttpStatus(code: Int, message: String) {
  def isOk: Boolean = code == HttpStatusCodes.SC_OK
}
