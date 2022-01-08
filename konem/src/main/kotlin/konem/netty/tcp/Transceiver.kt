package konem.netty.tcp

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

    abstract fun transmit(addr: SocketAddress, message: I, vararg extra: String)

    abstract fun receive(addr: SocketAddress, message: I, vararg extra: String)

    override fun toString(): String {
        return (
            "Transceiver{" + " channelReaders=" + channelReceiver.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}

abstract class ServerTransceiver<I>( channelPort: Int):Transceiver<I>(channelPort) {

    fun registerHandlerListener(listener: HandlerListener<I>) {
        if (!handlerListeners.contains(listener)) {
            handlerListeners.add(listener)
        }
    }

    abstract fun broadcast(message: I, vararg extra: String)

    override fun toString(): String {
        return (
            "ServerTransceiver{" + "activeHandlers=" + activeHandlers.size + ", channelReaders=" + channelReceiver.size +
                ", handlerListeners=" + handlerListeners.size + ", channelPort=" + channelPort + '}'.toString()
            )
    }
}
