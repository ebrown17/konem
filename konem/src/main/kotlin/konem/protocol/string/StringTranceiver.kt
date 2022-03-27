package konem.protocol.string


import konem.logger
import konem.netty.tcp.ServerTransceiver
import konem.netty.tcp.Transceiver
import java.net.SocketAddress


class StringTransceiver(channelPort: Int):Transceiver<String>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: String, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.trace("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: String, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.trace("receiver for {} is null", addr)
        }
    }
}

class StringServerTransceiver(channelPort: Int): ServerTransceiver<String>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: String, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.trace("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: String, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.trace("receiver for {} is null", addr)
        }
    }

    override fun broadcast(message: String, vararg extra: String) {
        logger.debug("message: {}", message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
