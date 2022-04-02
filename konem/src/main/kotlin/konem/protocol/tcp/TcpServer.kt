package konem.protocol.tcp

import io.netty.bootstrap.ServerBootstrap
import konem.logger
import konem.netty.*
import konem.netty.server.ServerChannelInfo
import konem.netty.server.ServerConfig
import konem.netty.server.ServerInternal
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap


class TcpServer<T> internal constructor(serverConfig: ServerConfig, heartbeatProtocol: ServerHeartbeatProtocol<T>,
                                       protocolPipeline: ProtocolPipeline<T>
): ServerInternal<T>(serverConfig,heartbeatProtocol,protocolPipeline) {

/*    companion object {
        fun create(config: (ServerConfig) -> Unit): Server<T> {
            val userConfig = ServerConfig()
            config(userConfig)
            val server = JsonServer(userConfig)
            for(port in userConfig.portSet){
                server.addChannel(port)
            }
            return server
        }
    }*/

    private val receiveListeners: ConcurrentHashMap<Int, ArrayList<MessageReceiver<T>>> =
        ConcurrentHashMap()

    private val logger = logger(this)

    override fun registerChannelReceiveListener(receiver: MessageReceiver<T>) {
        for (list in receiveListeners.values) {
            list.add(receiver)
        }
    }

    override fun registerChannelReceiveListener(port: Int, receiver: MessageReceiver<T>) {
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

    override fun broadcastOnChannel(port: Int, message: T, vararg args: String) {
        val transceiver = getTransceiverMap()[port]
        transceiver?.broadcast(message)
    }

    override fun broadcastOnAllChannels(message: T, vararg args: String) {
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

    override fun addChannel(port: Int, vararg args: String): Boolean {
        if (isPortConfigured(port)) {
            logger.warn("port {} already in use; not creating channel", port)
            return false
        }

        val transceiver = TcpServerTransceiver<T>(port)

        return if (addChannel(port, transceiver)) {
            receiveListeners[port] = ArrayList()
            true
        } else {
            false
        }
    }

    override fun createServerBootstrap(port: Int): ServerBootstrap {
        val transceiver = getTransceiverMap()[port]!!
        val channel = TcpServerChannel(transceiver,
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

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: T) {
        serverScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: T) {
        logger.trace("{}", message)
        val receiveListenerList = receiveListeners[port]
        if (receiveListenerList != null) {
            for (listener in receiveListenerList) {
                listener.handle(addr, message)
            }
        }
    }


}
