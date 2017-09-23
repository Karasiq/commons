package com.karasiq.common.configs

import scala.language.postfixOps

import com.typesafe.config.{Config, ConfigRenderOptions}

object ConfigEncoding {
  type EncodedConfigT = String

  private[this] val JSONOptions = ConfigRenderOptions.concise()
  private[this] val HOCONOptions = {
    ConfigRenderOptions.defaults()
      .setOriginComments(false)
      .setJson(false)
  }

  def toString(config: Config, json: Boolean = false): EncodedConfigT = {
    if (config.entrySet().isEmpty) {
      ""
    } else {
      val options = if (json) JSONOptions else HOCONOptions
      val configString = config.root().render(options)
      configString
    }
  }

  def toJson(config: Config): EncodedConfigT = {
    toString(config, json = true)
  }

  def toConfig(encConfig: EncodedConfigT): Config = {
    if (encConfig.nonEmpty) {
      Configs.fromString(encConfig)
    } else {
      ConfigUtils.emptyConfig
    }
  }

  def apply(values: (String, Any)*): EncodedConfigT = {
    encodeMap(values.toMap)
  }

  def encodeMap(map: Map[String, Any], json: Boolean = false): EncodedConfigT = {
    toString(Configs.fromMap(map), json)
  }

  def reformat(encConfig: EncodedConfigT, json: Boolean = false): EncodedConfigT = {
    toString(toConfig(encConfig), json)
  }
}
