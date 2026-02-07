package konem.netty

import konem.logger
import java.util.concurrent.ConcurrentHashMap

abstract class Transceiver<T>(protected val channelPort: Int) {

    private val logger = logger(javaClass)

    protected val activeHandlers: ConcurrentHashMap<ConnectionKey, Handler<T>> =
        ConcurrentHashMap()
    protected val activeLock = Any()

    protected val channelReceiver: ConcurrentHashMap<ConnectionKey, ChannelReceiver<T>> =
        ConcurrentHashMap()

    protected val handlerListeners: MutableList<HandlerListener<T>> = ArrayList()

    fun handlerActive(handler: Handler<T>) {
        synchronized(activeLock) {
            logger.trace("handler: {}", handler)
            val key = handler.connectionKey
            if (activeHandlers.putIfAbsent(key, handler) == null) {
                handlerListeners.forEach { it.registerActiveHandler(handler, channelPort) }
            }
        }
    }

    fun handlerInActive(handler: Handler<T>) {
        synchronized(activeLock) {
            logger.trace("handler: {}", handler)
            val handler = activeHandlers.remove(handler.connectionKey)
            if (handler != null) {
                handlerListeners.forEach { listener -> listener.registerInActiveHandler(handler, channelPort) }
            }
        }
    }

    fun registerChannelReceiver(connectionKey: ConnectionKey, receiver: ChannelReceiver<T>) {
        channelReceiver.putIfAbsent(connectionKey, receiver)
    }

    fun hasActiveHandler(connectionKey: ConnectionKey): Boolean {
        return activeHandlers.containsKey(connectionKey)
    }

    abstract fun transmit(connectionKey: ConnectionKey, message: T)

    abstract fun receive(connectionKey: ConnectionKey, message: T, extra: String)

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
