package konem.netty.stream

import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

open class Transceiver<T, H>(protected val channelPort: Int) {

  private val logger = LoggerFactory.getLogger(javaClass)

  protected val activeHandlers: ConcurrentHashMap<SocketAddress, Handler<H, T>> =
    ConcurrentHashMap()
  protected val channelReaders: ConcurrentHashMap<SocketAddress, ChannelReader> =
    ConcurrentHashMap()
  protected val handlerListeners: MutableList<HandlerListener<H, T>> = ArrayList()
  protected val activeLock = Any()

  fun handlerActive(addr: SocketAddress, handler: Handler<H, T>) {
    logger.info("remote: {}", addr)
    synchronized(activeLock) {
      val activeHandler = activeHandlers[addr]
      if (activeHandler == null) {
        activeHandlers.putIfAbsent(addr, handler)
        handlerListeners.forEach { listener -> listener.registerActiveHandler(handler, channelPort, addr) }
      }
    }
  }

  fun handlerInActive(addr: SocketAddress) {
    logger.info("handler inactive for remote: {}", addr)
    synchronized(activeLock) {
      val handler = activeHandlers.remove(addr)
      if (handler != null) {
        handlerListeners.forEach { listener -> listener.registerInActiveHandler(handler, channelPort, addr) }
      }
    }
  }

  fun registerChannelReader(addr: SocketAddress, reader: ChannelReader) {
    channelReaders.putIfAbsent(addr, reader)
  }

  fun registerHandlerListener(listener: HandlerListener<H, T>) {
    if (!handlerListeners.contains(listener)) {
      handlerListeners.add(listener)
    }
  }

  /**
   * Sends a message to specified address if connected to this transceiver
   * @param addr
   * @param message
   */
  fun transmit(addr: SocketAddress, message: H) {
    synchronized(activeLock) {
      logger.trace("to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      handler?.sendMessage(message)
    }
  }

  override fun toString(): String {
    return ("Transceiver{" + "activeHandlers=" + activeHandlers.size + ", channelReaders=" + channelReaders.size +
      ", handlerListeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString())
  }
}
