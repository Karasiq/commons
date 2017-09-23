package com.karasiq.common.configs

import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.util.ByteString
import com.typesafe.config.ConfigException

import com.karasiq.common.encoding.{Base64, ByteStringEncoding, HexString}

object ConfigImplicits extends ConfigImplicits

trait ConfigImplicits {
  type Config = com.typesafe.config.Config

  implicit class ConfigOps(config: Config) {
    def getFiniteDuration(path: String): FiniteDuration = {
      val duration = config.getDuration(path)
      FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
    }

    def getConfigIfExists(path: String): Config = {
      try {
        config.getConfig(path)
      } catch { case _: ConfigException ⇒
        ConfigUtils.emptyConfig
      }
    }

    def getConfigOrRef(path: String): Config = {
      try {
        config.getConfig(path)
      } catch { case _: ConfigException ⇒
        try {
          val path1 = config.getString(path)
          config.getConfig(path1)
        } catch { case _: ConfigException ⇒
          ConfigUtils.emptyConfig
        }
      }
    }

    def getClass[T](path: String): Class[T] = {
      Class.forName(config.getString(path)).asInstanceOf[Class[T]]
    }

    def getBytesInt(path: String): Int = {
      math.min(config.getBytes(path), Int.MaxValue).toInt
    }

    def getStrings(path: String): Seq[String] = {
      import scala.collection.JavaConverters._
      config.getStringList(path).asScala
    }

    def getStringSet(path: String): Set[String] = {
      getStrings(path).toSet
    }

    def getBytes(path: String, encoding: ByteStringEncoding = Base64): ByteString = {
      encoding.decode(config.getString(path))
    }

    def getHexString(path: String): ByteString = {
      getBytes(path, HexString)
    }

    def getUUID(path: String): UUID = {
      UUID.fromString(config.getString(path))
    }

    def optional[T](value: Config ⇒ T): Option[T] = {
      try {
        Option(value(config))
      } catch { case _: ConfigException ⇒
        None
      }
    }

    def withDefault[T](default: ⇒ T, value: Config ⇒ T): T = {
      optional(value).getOrElse(default)
    }
  }
}
