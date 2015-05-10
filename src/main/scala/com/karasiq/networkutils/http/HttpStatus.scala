package com.karasiq.networkutils.http

final case class HttpStatus(code: Int, message: String) {
  def isOk: Boolean = (200 until 300).contains(code) || message.equalsIgnoreCase("OK")
}
