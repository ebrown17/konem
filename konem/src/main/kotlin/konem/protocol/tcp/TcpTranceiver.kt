package konem.protocol.tcp

import konem.logger
import konem.netty.ConnectionKey
import konem.netty.ServerTransceiver
import konem.netty.Transceiver

class TcpTransceiver<T>(channelPort: Int) : Transceiver<T>(channelPort) {
    private val logger = logger(this)

    override fun transmit(connectionKey: ConnectionKey, message: T ) {
        synchronized(activeLock) {
            val handler = activeHandlers[connectionKey]
            logger.trace("{} with: {}", handler, message)
            handler?.sendMessage(message) ?: run {
                logger.warn("handler for {} is null", connectionKey)
            }
        }
    }

    override fun receive(connectionKey: ConnectionKey, message: T, extra: String) {
        logger.trace("from {} with {}", connectionKey.remoteAddress, message)
        val receiver = channelReceiver[connectionKey]
        receiver?.handleReceivedMessage(connectionKey, channelPort, message) ?: run {
            logger.warn("receiver for {} is null", connectionKey)
        }
    }
}

class TcpServerTransceiver<T>(channelPort: Int) : ServerTransceiver<T>(channelPort) {
    private val logger = logger(this)

    override fun transmit(connectionKey: ConnectionKey, message: T) {
        synchronized(activeLock) {
            val handler = activeHandlers[connectionKey]
            logger.trace("{} with: {}", handler, message)
            handler?.sendMessage(message) ?: run {
                logger.warn("handler for {} is null", connectionKey)
            }
        }
    }

    override fun receive(connectionKey: ConnectionKey, message: T, extra: String) {
        val receiver = channelReceiver[connectionKey]
        logger.trace("{} with {}", receiver, message)
        receiver?.handleReceivedMessage(connectionKey, channelPort, message) ?: run {
            logger.warn("receiver for {} is null", connectionKey)
        }
    }

    override fun broadcast(message: T, vararg extra: String) {
        logger.trace("message: {}", message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
