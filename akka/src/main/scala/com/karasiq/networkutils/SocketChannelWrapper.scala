package com.karasiq.networkutils

import java.io.{Closeable, IOException}
import java.nio.ByteBuffer
import java.nio.channels._

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, OneForOneStrategy, SupervisorStrategy}
import akka.io.Tcp
import akka.io.Tcp.{PeerClosed, Received, Write}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils

import scala.collection.concurrent.TrieMap
import scala.language.implicitConversions
import scala.util.control.Exception.catching

object SocketChannelWrapper {
  private val config = ConfigFactory.load().getConfig("karasiq.commons.socket-channel-wrapper")
  private[networkutils] val bufferSize = config.getInt("read-buffer-size")

  def writer(sc: SocketChannel): SocketChannelWriteWrapper = new SocketChannelWriteWrapper {
    override def onWrite(data: ByteString, ack: Tcp.Event): Unit = {
      sc.write(data.toByteBuffer)
      if (ack != Tcp.NoAck) {
        sender() ! ack
      }
    }
  }

  private val readMap = TrieMap.empty[SelectionKey, ActorRef]
  private val readWrapper = new SocketChannelReadWrapper {
    override def onRead(key: SelectionKey, b: ByteString): Unit = {
      readMap.get(key).foreach(_ ! Received(b))
    }

    override def onClose(key: SelectionKey): Unit = {
      readMap.remove(key).foreach(_ ! PeerClosed)
    }
  }

  def register(sc: SocketChannel, listener: ActorRef): Unit = {
    assert(sc.finishConnect())
    sc.configureBlocking(false)
    val key = sc.register(readWrapper.selector.wakeup(), SelectionKey.OP_READ)
    readMap += (key → listener)
  }

  def unregister(sc: SocketChannel): Option[ActorRef] = {
    Option(sc.keyFor(readWrapper.selector)).flatMap { key ⇒
      key.cancel()
      readMap.remove(key)
    }
  }

  /**
   * Provides immutable read-write ops for [[java.nio.channels.SocketChannel]]
   */
  implicit class ReadWriteOps(sc: SocketChannel) {
    private implicit def toByteBuffer(b: Seq[Byte]): ByteBuffer = ByteBuffer.wrap(b.toArray)

    def read(bufferSize: Int = SocketChannelWrapper.bufferSize): ByteString = {
      val buffer = ByteBuffer.allocate(bufferSize)
      sc.read(buffer)
      buffer.flip()
      ByteString(buffer)
    }

    def write(data: Seq[Byte]): Int = {
      sc.write(toByteBuffer(data))
    }

    def writeRead(data: Seq[Byte], bufferSize: Int = SocketChannelWrapper.bufferSize): ByteString = {
      write(data)
      read(bufferSize)
    }
  }
}

abstract class SocketChannelWriteWrapper extends Actor {
  def onWrite(data: ByteString, ack: Tcp.Event): Unit

  override def receive: Receive = {
    case Write(data, ack) ⇒
      onWrite(data, ack)
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: IOException ⇒ Stop
  }
}

abstract class SocketChannelReadWrapper extends Closeable {
  def onRead(key: SelectionKey, b: ByteString): Unit
  def onClose(key: SelectionKey): Unit

  val selector = Selector.open()

  private def readKey(key: SelectionKey, buffer: ByteBuffer): Unit = {
    catching(classOf[IOException], classOf[CancelledKeyException]).withApply { _ ⇒ key.cancel(); onClose(key) } {
      if (key.isReadable && key.channel().isInstanceOf[SocketChannel]) {
        val channel: SocketChannel = key.channel().asInstanceOf[SocketChannel]
        val size = channel.read(buffer)
        size match {
          case -1 ⇒ // EOF
            key.cancel()
            onClose(key)

          case 0 ⇒
          // Pass

          case _ ⇒
            buffer.flip()
            onRead(key, ByteString(buffer))
        }
      }
    }
  }

  private def runnable: Runnable = {
    new Runnable {
      override def run(): Unit = {
        try {
          val buffer: ByteBuffer = ByteBuffer.allocate(SocketChannelWrapper.bufferSize)
          while (!Thread.currentThread().isInterrupted) {
            selector.select()
            val iterator = selector.selectedKeys().iterator()
            while (iterator.hasNext) {
              val key: SelectionKey = iterator.next()
              buffer.clear()
              readKey(key, buffer)
              iterator.remove()
            }
          }
        } catch {
          case _: InterruptedException | _: ClosedByInterruptException ⇒
        }
      }
    }
  }

  private val thread = new Thread(runnable)
  thread.setName("SocketChannelWrapper")
  thread.setDaemon(true)
  thread.start()

  override def close(): Unit = {
    thread.interrupt()
    IOUtils.closeQuietly(selector)
  }
}
