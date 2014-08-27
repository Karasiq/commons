package com.karasiq.common

object StringUtils {
  def htmlTrim(source: String): String = source.replaceAll("(^[\\s\\u00a0]+|[\\s\\u00a0]+$)", "") // &nbsp
  def repeated(source: String, count: Int): String = source * count
}
