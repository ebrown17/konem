package konem.protocol.websocket


import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.data.protobuf.KonemMessage
import konem.logger
import konem.netty.ServerTransceiver
import konem.netty.Transceiver
import java.net.SocketAddress


class WebSocketTransceiver<T>(channelPort: Int) : Transceiver<T>(channelPort) {
    private val logger = logger(this)

    private val konemSerializer = KonemMessageSerializer()
    private val protoSerializer = KonemMessage()

    override fun transmit(addr: SocketAddress, message: T, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}", handler, addr, message)
            handler?.sendMessage(message) ?: run {
                logger.warn("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: T, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr]
        receiver?.handleReceivedMessage(addr, channelPort, message) ?: run {
            logger.warn("receiver for {} is null", addr)
        }
    }
}

class WebSocketServerTransceiver<T>(channelPort: Int) : ServerTransceiver<T>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: T, vararg extra: String) {
        synchronized(activeLock) {
            val handler = activeHandlers[addr]
            logger.trace("{} to addr: {} with: {}", handler, addr, message)
            handler?.sendMessage(message) ?: run {
                logger.warn("handler for {} is null", addr)
            }
        }
    }

    override fun receive(addr: SocketAddress, message: T, vararg extra: String) {
        val receiver = channelReceiver[addr]
        logger.trace("{} from {} with {}", receiver, addr, message)
        receiver?.handleReceivedMessage(addr, channelPort, message) ?: run {
            logger.warn("receiver for {} is null", addr)
        }
    }

    override fun broadcast(message: T, vararg webSocketPaths: String) {
        logger.debug("paths:{} message: {}", webSocketPaths, message)
        val frame = when(message) {
            is Message ->
        }
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
