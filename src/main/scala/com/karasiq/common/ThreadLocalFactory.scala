package com.karasiq.common

import java.io.Closeable

import com.karasiq.common.factory.Factory

import scala.ref.WeakReference

abstract class ThreadLocalFactory[T] extends Factory[T] with Closeable

private sealed abstract class ThreadLocalFactoryImpl[T] extends ThreadLocalFactory[T] {
  protected def newInstance(): T
  protected def closeInstance(instance: T): Unit

  protected final val threadLocal = new ThreadLocal[T] {
    override def initialValue(): T = newInstance()
  }

  override def apply(): T = {
    threadLocal.get()
  }

  override def close(): Unit = {
    closeInstance(apply())
    threadLocal.remove()
  }
}

private sealed abstract class ThreadLocalWeakFactoryImpl[T <: AnyRef] extends ThreadLocalFactory[T] {
  protected def newInstance(): T
  protected def closeInstance(instance: T): Unit

  protected final val threadLocal = new ThreadLocal[WeakReference[T]] {
    override def initialValue(): WeakReference[T] = WeakReference[T](newInstance())
  }

  override def apply(): T = {
    val wr = threadLocal.get().get
    if (wr.isEmpty) {
      threadLocal.remove()
      threadLocal.get()() // Re-create object
    } else {
      wr.get
    }
  }

  override def close(): Unit = {
    if (threadLocal.get().get.nonEmpty) {
      try { closeInstance(threadLocal.get()()) }
      finally { threadLocal.remove() }
    }
  }
}

object ThreadLocalFactory {
  def apply[T](newInstanceFunction: ⇒ T, closeInstanceFunction: T ⇒ Unit = (_: T) ⇒ ()): ThreadLocalFactory[T] = new ThreadLocalFactoryImpl[T] {
    override protected def newInstance(): T = newInstanceFunction

    override protected def closeInstance(instance: T): Unit = closeInstanceFunction(instance)
  }

  def weakRef[T <: AnyRef](newInstanceFunction: ⇒ T, closeInstanceFunction: T ⇒ Unit = (_: T) ⇒ ()): ThreadLocalFactory[T] = new ThreadLocalWeakFactoryImpl[T] {
    override protected def newInstance(): T = newInstanceFunction

    override protected def closeInstance(instance: T): Unit = closeInstanceFunction(instance)
  }
}
