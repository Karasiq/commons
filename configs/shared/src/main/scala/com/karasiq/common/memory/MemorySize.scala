package com.karasiq.common.memory

import scala.language.{implicitConversions, postfixOps}

@SerialVersionUID(0L)
final case class SizeUnit(name: String, value: BigInt) {
  val longValue: Long = if (value.isValidLong) value.longValue() else Long.MaxValue
  val intValue: Int = if (value.isValidInt) value.intValue() else Int.MaxValue

  override def toString: String = name
}

object SizeUnit {
  private[this] val _1024 = 1024L

  val bytes = SizeUnit("bytes", 1)
  val KB = SizeUnit("KB", _1024)
  val MB = SizeUnit("MB", KB.value * _1024)
  val GB = SizeUnit("GB", MB.value * _1024)
  val TB = SizeUnit("TB", GB.value * _1024)
  val PB = SizeUnit("PB", TB.value * _1024)
  val EB = SizeUnit("EB", PB.value * _1024)
  val ZB = SizeUnit("ZB", EB.value * _1024)
  val YB = SizeUnit("YB", ZB.value * _1024)

  val Units = Array(
    bytes, KB, MB, GB, TB, PB, EB, ZB, YB
  )

  private[this] val UnitsMap = Units.map(u ⇒ (u.name, u)).toMap

  def fromString(unit: String): SizeUnit = {
    UnitsMap.get(unit)
      .orElse(Units.find(_.name.equalsIgnoreCase(unit)))
      .getOrElse(throw new NoSuchElementException(unit))
  }

  implicit def toLong(sizeUnit: SizeUnit): Long = {
    sizeUnit.longValue
  }
}

sealed trait MemorySize {
  def toBytes: Long
  def toBytesInt: Int
  def toBytesBigInt: BigInt
}

object MemorySize {
  // -----------------------------------------------------------------------
  // Constants
  // -----------------------------------------------------------------------
  // private[this] val DecimalFormat = new DecimalFormat("#0.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)) // Scala.js linking error
  private[this] val _1024 = 1024L
  private[this] val Log10_1024 = Math.log10(_1024)
  private[this] val UnitPows = SizeUnit.Units.indices.map(Math.pow(_1024, _)).toArray

  // -----------------------------------------------------------------------
  // Types
  // -----------------------------------------------------------------------
  @SerialVersionUID(0L)
  final case class Exact(amount: Long, unit: SizeUnit) extends MemorySize {
    def toBytes: Long = unit.longValue * amount
    def toBytesInt: Int = unit.intValue * amount.toInt
    def toBytesBigInt: BigInt = unit.value * amount

    override def toString: String = {
      // s"$amount $unit"
      val sb = new StringBuilder(8)
      sb.append(amount).append(' ').append(unit)
      sb.result()
    }
  }

  @SerialVersionUID(0L)
  final case class Coarse(amount: Double, unit: SizeUnit) extends MemorySize {
    def toBytes: Long = (amount * unit.longValue).toLong
    def toBytesInt: Int = (amount * unit.intValue).toInt
    def toBytesBigInt: BigInt = (BigDecimal(unit.value) * amount).toBigInt()

    override def toString: String = {
      // DecimalFormat.format(amount) + " " + unit
      val sb = new StringBuilder(8)
      sb ++= f"$amount%.2f" += ' ' ++= unit.toString
      sb.result()
    }      
  }

  // -----------------------------------------------------------------------
  // Conversions
  // -----------------------------------------------------------------------
  def apply(bytes: Long): MemorySize = {
    if (bytes <= _1024) {
      Exact(bytes, SizeUnit.bytes)
    } else {
      val unitIndex: Int = math.max(0, math.min(SizeUnit.Units.length - 1, (Math.log10(bytes) / Log10_1024).toInt))
      Coarse(bytes.toDouble / UnitPows(unitIndex), SizeUnit.Units(unitIndex))
    }
  }

  def toString(bytes: Long): String = {
    apply(bytes).toString
  }
}
