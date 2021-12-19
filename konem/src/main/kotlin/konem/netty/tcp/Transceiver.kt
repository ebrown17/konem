package konem.netty.tcp

import org.slf4j.LoggerFactory
import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap

open class Transceiver<I>(protected val channelPort: Int) {

    private val logger = LoggerFactory.getLogger(javaClass)

    protected val activeHandlers: ConcurrentHashMap<SocketAddress, Handler<I>> =
        ConcurrentHashMap()
    protected val channelReceiver: ConcurrentHashMap<SocketAddress, ChannelReceiver<I>> =
        ConcurrentHashMap()
    protected val handlerListeners: MutableList<HandlerListener<I>> = ArrayList()
    protected val activeLock = Any()

    fun handlerActive(addr: SocketAddress, handler: Handler<I>) {
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

    fun registerChannelReceiver(addr: SocketAddress, reader: ChannelReceiver<I>) {
        channelReceiver.putIfAbsent(addr, reader)
    }

    fun registerHandlerListener(listener: HandlerListener<I>) {
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
            logger.trace("to addr: {} with {}", addr, message)
            val handler = activeHandlers[addr]
            handler?.sendMessage(message)
        }
    }

    override fun toString(): String {
        return (
            "Transceiver{" + "activeHandlers=" + activeHandlers.size + ", channelReaders=" + channelReceiver.size +
                ", handlerListeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}
