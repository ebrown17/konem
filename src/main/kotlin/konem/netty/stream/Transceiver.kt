package konem.netty.stream

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

open class Transceiver<I>(protected val channelPort: Int) {

  private val logger = LoggerFactory.getLogger(javaClass)

  protected val activeHandlers: ConcurrentHashMap<InetSocketAddress, Handler<I>> =
    ConcurrentHashMap()
  protected val channelReaders: ConcurrentHashMap<InetSocketAddress, ChannelReader> =
    ConcurrentHashMap()
  protected val handlerListeners: MutableList<HandlerListener> = ArrayList()
  protected val activeLock = Any()

  fun handlerActive(addr: InetSocketAddress, handler: Handler<I>) {
    logger.info("handler active remote: {}", addr)
    synchronized(activeLock) {
      val activeHandler = activeHandlers[addr]
      if (activeHandler == null) {
        activeHandlers.putIfAbsent(addr, handler)
        handlerListeners.forEach { listener -> listener.registerActiveHandler(channelPort, addr) }
      }
    }
  }

  fun handlerInActive(addr: InetSocketAddress) {
    logger.info("registerHandlerInActive handler inactive, remote: {}", addr)
    synchronized(activeLock) {
      activeHandlers.remove(addr)
      handlerListeners.forEach { listener -> listener.registerInActiveHandler(channelPort, addr) }
    }
  }

  fun registerChannelReader(addr: InetSocketAddress, reader: ChannelReader) {
    channelReaders.putIfAbsent(addr, reader)
  }

  fun registerHandlerListener(listener: HandlerListener) {
    if (!handlerListeners.contains(listener)) {
      handlerListeners.add(listener)
    }
  }

  fun transmit(addr: InetSocketAddress, message: I) {
    synchronized(activeLock) {
      logger.debug("sendMessage to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      handler?.sendMessage(message)
    }
  }

  override fun toString(): String {
    return ("Transceiver{" + "activeHandlers=" + activeHandlers.size + ", channelReaders=" + channelReaders.size
      + ", handlerListeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString())
  }
}
