package com.karasiq.common.encoding

import java.nio.ByteOrder
import java.util.UUID

import akka.util.ByteString

object UUIDEncoding {
  def toBytes(uuid: UUID): ByteString = {
    implicit val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    val builder = ByteString.newBuilder
    builder.sizeHint(16)
    builder.putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
      .result()
  }

  def fromBytes(bytes: ByteString): UUID = {
    val bb = bytes.toByteBuffer
    new UUID(bb.getLong, bb.getLong)
  }
}
