package konem.protocol.websocket

import konem.logger
import konem.netty.ConnectionKey
import konem.netty.MessageReceiver
import konem.netty.client.ClientBootstrapConfig
import konem.netty.client.ClientInternal
import konem.netty.client.WebSocketClient
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList

class WebSocketClientImp<T>(
    private val serverAddress: SocketAddress,
    config: ClientBootstrapConfig<T>,
    private val fullWebSocketPath: URI
):
    ClientInternal<T>(serverAddress,config), WebSocketClient<T> {

    private val logger = logger(this)
    private val transceiver = config.transceiver
    private val receiveListeners = CopyOnWriteArrayList<MessageReceiver<T>>()

    override fun sendMessage(message: T) {
        if (!isActive()) {
            logger.warn("attempted to send data on null or closed channel")
            return
        }
        logger.info("remote: {} message: {}", channel?.remoteAddress(), message)
        transceiver.transmit(serverConnectionKey, message)
    }

    override fun isActive(): Boolean {
        return super.isActive() && transceiver.hasActiveHandler(serverConnectionKey)
    }

    override fun registerChannelMessageReceiver(receiver: MessageReceiver<T>) {
        receiveListeners.add(receiver)
    }

    override fun handleReceivedMessage(connectionKey: ConnectionKey, port: Int, message: T, extra: String) {
        clientScope.launch {
            receiveMessage(connectionKey, port, message,extra)
        }
    }

    override suspend fun receiveMessage(connectionKey: ConnectionKey, port: Int, message: T, extra: String) {
        logger.trace("got message: {} for path: {}", message,extra)
        for (listener in receiveListeners) {
            listener.handle(connectionKey, message)
        }
    }

    override fun toString(): String {
        return "WebSocketClient{Path=$fullWebSocketPath, $transceiver}"
    }

    override fun registerChannelMessageReceiver(
        receiver: MessageReceiver<T>,
        vararg webSocketPaths: String
    ) {
        registerChannelMessageReceiver(receiver)
    }

}
