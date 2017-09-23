package com.karasiq.common.configs

import scala.collection.JavaConverters._

import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}

object Configs {
  def apply(values: (String, Any)*): Config = {
    fromMap(values.toMap)
  }

  def fromMap(map: Map[String, Any]): Config = {
    ConfigFactory.parseMap(map.asJava)
  }

  def fromString(str: String): Config = {
    ConfigFactory.parseString(str)
  }

  def fromBytes(bytes: ByteString): Config = {
    fromString(bytes.utf8String)
  }
}
