package konem.protocol.konem.wire


import konem.data.protobuf.KonemMessage
import konem.logger
import konem.netty.ServerTransceiver
import konem.netty.Transceiver
import java.net.SocketAddress


class WireTransceiver(channelPort: Int): Transceiver<KonemMessage>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: KonemMessage, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.trace("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: KonemMessage, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.trace("receiver for {} is null", addr)
        }
    }
}

class WireServerTransceiver(channelPort: Int): ServerTransceiver<KonemMessage>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: KonemMessage, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.trace("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: KonemMessage, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.trace("receiver for {} is null", addr)
        }
    }

    override fun broadcast(message: KonemMessage, vararg extra: String) {
        logger.debug("message: {}", message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
