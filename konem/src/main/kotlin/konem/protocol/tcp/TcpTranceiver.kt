package konem.protocol.tcp


import konem.logger
import konem.netty.ServerTransceiver
import konem.netty.Transceiver
import java.net.SocketAddress


class TcpTransceiver<I>(channelPort: Int): Transceiver<I>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: I, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.warn("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: I, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.warn("receiver for {} is null", addr)
        }
    }
}

class TcpServerTransceiver<I>(channelPort: Int): ServerTransceiver<I>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: I, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}",handler, addr, message)
            handler?.sendMessage(message)?: run {
                logger.warn("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: I, vararg extra: String) {
        val receiver = channelReceiver[addr]
        logger.trace("{} from {} with {}", receiver, addr, message)
        receiver?.handleReceivedMessage(addr, channelPort, message)?: run {
            logger.warn("receiver for {} is null", addr)
        }
    }

    override fun broadcast(message: I, vararg extra: String) {
        logger.trace("message: {}", message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
