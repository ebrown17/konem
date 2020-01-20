package konem.netty.stream

import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

open class Transceiver<I>(protected val channelPort: Int) {

  private val logger = LoggerFactory.getLogger(javaClass)

  protected val activeHandlers: ConcurrentHashMap<SocketAddress, Handler<I>> =
    ConcurrentHashMap()
  protected val channelReaders: ConcurrentHashMap<SocketAddress, ChannelReader> =
    ConcurrentHashMap()
  protected val handlerListeners: MutableList<HandlerListener> = ArrayList()
  protected val activeLock = Any()

  fun handlerActive(addr: SocketAddress, handler: Handler<I>) {
    logger.info("remote: {}", addr)
    synchronized(activeLock) {
      val activeHandler = activeHandlers[addr]
      if (activeHandler == null) {
        activeHandlers.putIfAbsent(addr, handler)
        handlerListeners.forEach { listener -> listener.registerActiveHandler(channelPort, addr) }
      }
    }
  }

  fun handlerInActive(addr: SocketAddress) {
    logger.info("handler inactive for remote: {}", addr)
    synchronized(activeLock) {
      activeHandlers.remove(addr)
      handlerListeners.forEach { listener -> listener.registerInActiveHandler(channelPort, addr) }
    }
  }

  fun registerChannelReader(addr: SocketAddress, reader: ChannelReader) {
    channelReaders.putIfAbsent(addr, reader)
  }

  fun registerHandlerListener(listener: HandlerListener) {
    if (!handlerListeners.contains(listener)) {
      handlerListeners.add(listener)
    }
  }

  /**
   * Sends a message to specified address if connected to this transceiver
   * @param addr
   * @param message
   */
  fun transmit(addr: SocketAddress, message: I) {
    synchronized(activeLock) {
      logger.debug("to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      handler?.sendMessage(message)
    }
  }

  override fun toString(): String {
    return ("Transceiver{" + "activeHandlers=" + activeHandlers.size + ", channelReaders=" + channelReaders.size +
      ", handlerListeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString())
  }
}
