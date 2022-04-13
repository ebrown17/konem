package konem.netty

import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import konem.logger

abstract class Transceiver<T>(protected val channelPort: Int) {

    private val logger = logger(javaClass)

    protected val activeHandlers: ConcurrentHashMap<SocketAddress, Handler<T>> =
        ConcurrentHashMap()
    protected val activeLock = Any()

    protected val channelReceiver: ConcurrentHashMap<SocketAddress, ChannelReceiver<T>> =
        ConcurrentHashMap()

    protected val handlerListeners: MutableList<HandlerListener<T>> = ArrayList()

    fun handlerActive(addr: SocketAddress, handler: Handler<T>) {
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

    fun registerChannelReceiver(addr: SocketAddress, receiver: ChannelReceiver<T>) {
        channelReceiver.putIfAbsent(addr, receiver)
    }

    abstract fun transmit(addr: SocketAddress, message: T, vararg extra: String)

    abstract fun receive(addr: SocketAddress, message: T, vararg extra: String)

    override fun toString(): String {
        return (
            "Transceiver{" + " receivers=" + channelReceiver.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}

abstract class ServerTransceiver<T>(channelPort: Int) : Transceiver<T>(channelPort) {

    fun registerHandlerListener(listener: HandlerListener<T>) {
        if (!handlerListeners.contains(listener)) {
            handlerListeners.add(listener)
        }
    }

    abstract fun broadcast(message: T, vararg extra: String)

    override fun toString(): String {
        return (
            "{" + "activeHandlers=" + activeHandlers.size + ", receivers=" + channelReceiver.size +
                ", listeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}
