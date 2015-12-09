package com.karasiq.common

import java.util.concurrent.atomic.AtomicReference

import com.karasiq.common.factory.Factory

import scala.annotation.tailrec
import scala.ref.WeakReference

abstract sealed class WeakRefFactory[T <: AnyRef] extends Factory[T] {
  protected def newInstance(): T

  private val value = new AtomicReference[WeakReference[T]](WeakReference(newInstance()))

  @tailrec
  private def setNewInstance(oldValue: WeakReference[T], newValue: T): T = {
    if (value.compareAndSet(oldValue, WeakReference(newValue))) newValue
    else this.setNewInstance(oldValue, newValue)
  }

  /**
   * Creates new object or returns cached
   * @return Created object
   */
  override def apply(): T = {
    val ref = value.get()
    ref.get.getOrElse(setNewInstance(ref, newInstance()))
  }
}

object WeakRefFactory {
  def apply[T <: AnyRef](newInstanceFunction: ⇒ T): WeakRefFactory[T] = new WeakRefFactory[T] {
    override protected def newInstance(): T = {
      newInstanceFunction
    }
  }
}
