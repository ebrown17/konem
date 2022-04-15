package konem.protocol.websocket

import io.netty.bootstrap.ServerBootstrap
import konem.logger
import konem.netty.*
import konem.netty.server.*
import konem.protocol.websocket.json.WebSocketConnectionListener
import konem.protocol.websocket.json.WebSocketConnectionStatusListener
import konem.protocol.websocket.json.WebSocketDisconnectionListener
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap


class WebSocketServerImp<T> internal constructor(
    serverConfig: WebSocketServerConfig, heartbeatProtocol: ServerHeartbeatProtocol<T>,
    protocolPipeline: ProtocolPipeline<T>
) : WebSocketServerInternal<T>(serverConfig, heartbeatProtocol, protocolPipeline), WebSocketServer<T> {

    private val receiveListeners: ConcurrentHashMap<Int, ArrayList<MessageReceiver<T>>> =
        ConcurrentHashMap()

    private val logger = logger(this)

    override fun registerChannelMessageReceiver(receiver: MessageReceiver<T>) {
        for (list in receiveListeners.values) {
            list.add(receiver)
        }
    }

    override fun registerChannelMessageReceiver(port: Int, receiver: MessageReceiver<T>) {
        if (!isPortConfigured(port)) {
            throw IllegalArgumentException("port type can't be null or port is not configured: port $port")
        }

        var readerListenerList = receiveListeners[port]
        if (readerListenerList == null) {
            readerListenerList = arrayListOf()
        }
        readerListenerList.add(receiver)
        receiveListeners[port] = readerListenerList
    }

    override fun broadcastOnChannel(port: Int, message: T, vararg webSocketPaths: String) {
        val transceiver = getTransceiverMap()[port]
        transceiver?.broadcast(message)
    }

    override fun broadcastOnAllChannels(message: T, vararg webSocketPaths: String) {
        val transceiverMap = getTransceiverMap()
        for (transceiver in transceiverMap.values) {
            transceiver.broadcast(message)
        }
    }

    override fun sendMessage(addr: SocketAddress, message: T) {
        val channelPort = getRemoteHostToChannelMap()[addr]
        if (channelPort != null) {
            val transceiver = getTransceiverMap()[channelPort]
            transceiver?.transmit(addr, message)
        }
    }

    override fun addChannel(port: Int, vararg websocketPaths: String): Boolean {
        if (isPortConfigured(port)) {
            logger.warn("port {} already in use; not creating channel", port)
            return false
        }

        val validPaths = hashSetOf(*websocketPaths)

        for (path in websocketPaths) {
            if (isPathConfiguredOnPort(port, path)) {
                validPaths.remove(path)
            }
        }

        if (validPaths.isNotEmpty()) {
            val transceiver = WebSocketServerTransceiver<T>(port)
            websocketMap.putIfAbsent(port, validPaths.toTypedArray())
            if (addChannel(port, transceiver)) {
                receiveListeners[port] = ArrayList()
                return true
            } else {
                websocketMap.remove(port)
            }
        }

        return false
    }

    override fun createServerBootstrap(port: Int): ServerBootstrap {
        val transceiver = getTransceiverMap()[port]!!
        val channel = WebSocketServerChannel(
            transceiver,
            ServerChannelInfo(
                serverConfig.USE_SSL,
                serverConfig.CHANNEL_IDS.incrementAndGet(),
                heartbeatProtocol,
                protocolPipeline
            )
        )
        return createServerBootstrap(channel)
    }

    override fun connectionActive(handler: Handler<T>) {
        for (listener in connectionListeners) {
            listener.onConnection(handler.remoteAddress)
        }
    }

    override fun connectionInActive(handler: Handler<T>) {
        for (listener in disconnectionListeners) {
            listener.onDisconnection(handler.remoteAddress)
        }
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: T, extra: String) {
        serverScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: T, extra: String) {
        logger.trace("{}", message)
        val receiveListenerList = receiveListeners[port]
        if (receiveListenerList != null) {
            for (listener in receiveListenerList) {
                listener.handle(addr, message)
            }
        }
    }

    override fun registerChannelReadListener(receiver: MessageReceiver<T>, vararg webSocketPaths: String) {

    }

    override fun registerChannelReadListener(port: Int, receiver: MessageReceiver<T>, vararg webSocketPaths: String) {

    }

    override fun registerPathConnectionListener(listener: WebSocketConnectionListener){

    }

    override fun registerPathDisconnectionListener(listener: WebSocketDisconnectionListener){

    }

    override fun registerPathConnectionStatusListener(listener: WebSocketConnectionStatusListener){

    }

}
