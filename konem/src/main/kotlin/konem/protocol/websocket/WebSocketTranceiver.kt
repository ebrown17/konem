package konem.protocol.websocket


import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.data.protobuf.KonemMessage
import konem.logger
import konem.netty.ConnectionKey
import konem.netty.ServerTransceiver
import konem.netty.Transceiver
import java.net.SocketAddress


class WebSocketTransceiver<T>(channelPort: Int) : Transceiver<T>(channelPort) {
    private val logger = logger(this)

    private val konemSerializer = KonemMessageSerializer()

    override fun transmit(connectionKey: ConnectionKey, message: T) {
        synchronized(activeLock) {
            val handler = activeHandlers[connectionKey]
            logger.trace("{} with: {}", handler, message)
            handler?.sendMessage(message) ?: run {
                logger.warn("handler for {} is null", connectionKey)
            }
        }
    }

    override fun receive(connectionKey: ConnectionKey, message: T,  webSocketPath: String) {
        logger.trace("from {} with {}", connectionKey, message)
        val receiver = channelReceiver[connectionKey]
        receiver?.handleReceivedMessage(connectionKey, channelPort, message) ?: run {
            logger.warn("receiver for {} is null", connectionKey)
        }
    }
}

class WebSocketServerTransceiver<T>(channelPort: Int) : ServerTransceiver<T>(channelPort) {
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

    override fun receive(connectionKey: ConnectionKey, message: T,  webSocketPath: String) {
        val receiver = channelReceiver[connectionKey]
        logger.trace("{} with {}", receiver,  message)
        receiver?.handleReceivedMessage(connectionKey, channelPort, message,webSocketPath) ?: run {
            logger.warn("receiver for {} is null", connectionKey)
        }
    }

    override fun broadcast(message: T, vararg webSocketPaths: String) {
        logger.trace("paths:{} message: {}", webSocketPaths, message)
        synchronized(activeLock) {
            if (webSocketPaths.isEmpty()) {
                for (handler in activeHandlers.values) {
                    handler.sendMessage(message)
                }
            } else {
                val pathSet = webSocketPaths.toHashSet()
                for (handler in activeHandlers.values) {
                    if (handler is WebSocketHandler<*> && handler.webSocketPath in pathSet) {
                        handler.sendMessage(message)
                    }
                }
            }
        }
    }
}
