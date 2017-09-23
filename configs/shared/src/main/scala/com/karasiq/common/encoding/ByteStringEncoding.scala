package com.karasiq.common.encoding

trait ByteStringEncoding {
  final type BytesT = akka.util.ByteString
  final type EncodedT = String

  def encode(bytes: BytesT): EncodedT
  def decode(string: EncodedT): BytesT
}
