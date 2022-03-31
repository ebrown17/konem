package konem.netty

import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import konem.logger

abstract class Transceiver<I>(protected val channelPort: Int) {

    private val logger = logger(javaClass)

    protected val activeHandlers: ConcurrentHashMap<SocketAddress, Handler<I>> =
        ConcurrentHashMap()
    protected val activeLock = Any()

    protected val channelReceiver: ConcurrentHashMap<SocketAddress, ChannelReceiver<I>> =
        ConcurrentHashMap()

    protected val handlerListeners: MutableList<HandlerListener<I>> = ArrayList()

    fun handlerActive(addr: SocketAddress, handler: Handler<I>) {
        synchronized(activeLock) {
            logger.trace("handlerActive remote: {}", addr)
            val activeHandler = activeHandlers[addr]
            if (activeHandler == null) {
                activeHandlers.putIfAbsent(addr, handler)
                handlerListeners.forEach { listener -> listener.registerActiveHandler(handler, channelPort, addr) }
            }
        }
    }

    fun handlerInActive(addr: SocketAddress) {
        synchronized(activeLock) {
            logger.trace("remote: {}", addr)
            val handler = activeHandlers.remove(addr)
            if (handler != null) {
                handlerListeners.forEach { listener -> listener.registerInActiveHandler(handler, channelPort, addr) }
            }
        }
    }

    fun registerChannelReceiver(addr: SocketAddress, reader: ChannelReceiver<I>) {
        channelReceiver.putIfAbsent(addr, reader)
    }

    abstract fun transmit(addr: SocketAddress, message: I, vararg extra: String)

    abstract fun receive(addr: SocketAddress, message: I, vararg extra: String)

    override fun toString(): String {
        return (
            "Transceiver{" + " receivers=" + channelReceiver.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}

abstract class ServerTransceiver<I>( channelPort: Int): Transceiver<I>(channelPort) {

    fun registerHandlerListener(listener: HandlerListener<I>) {
        if (!handlerListeners.contains(listener)) {
            handlerListeners.add(listener)
        }
    }

    abstract fun broadcast(message: I, vararg extra: String)

    override fun toString(): String {
        return (
            "{" + "activeHandlers=" + activeHandlers.size + ", receivers=" + channelReceiver.size +
                ", listeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}
