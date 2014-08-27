package com.karasiq.common

import java.util.concurrent.atomic.AtomicReference

import scala.language.implicitConversions

object Lazy {
  def apply[A](f: => A): Lazy[A] = new LazyImpl[A](f)

  def atomic[A](f: => A): Lazy[A] = new LazyAtomicImpl[A](f)

  implicit def evalLazy[A](l: Lazy[A]): A = l()
}

abstract class Lazy[A](f: => A) {
  protected def option: Option[A]
  protected def option_=(v: Option[A]): Unit

  def apply(): A = option match {
    case Some(a) => a
    case None =>
      val newValue = f
      option = Some(newValue)
      newValue
  }

  def isDefined: Boolean = option.isDefined

  def ifDefined(function: A => Unit): Unit = {
    option.foreach(function)
  }
}

class LazyImpl[A](f: => A) extends Lazy[A](f) {
  override protected var option: Option[A] = None
}

class LazyAtomicImpl[A](f: => A) extends Lazy[A](f) {
  private val atomicRef = new AtomicReference[Option[A]](None)

  override protected def option = atomicRef.get()

  override protected def option_=(v: Option[A]) = atomicRef.compareAndSet(null, v)
}