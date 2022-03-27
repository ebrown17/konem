package konem.protocol.string

import io.netty.bootstrap.ServerBootstrap
import konem.logger
import konem.netty.tcp.Handler
import konem.netty.tcp.Receiver
import konem.netty.tcp.server.Server
import konem.netty.tcp.server.ServerChannelInfo
import konem.netty.tcp.server.ServerConfig
import konem.netty.tcp.server.ServerInternal
import kotlinx.coroutines.launch
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap


class StringServer private constructor(serverConfig: ServerConfig): ServerInternal<String>(serverConfig),StringChannelReceiver {

    companion object {
        fun create(config: (ServerConfig) -> Unit): Server<String> {
            val userConfig = ServerConfig()
            config(userConfig)
            val server = StringServer(userConfig)
            for(port in userConfig.portSet){
                server.addChannel(port)
            }
            return server
        }
    }

    private val receiveListeners: ConcurrentHashMap<Int, ArrayList<Receiver<String>>> =
        ConcurrentHashMap()

    private val logger = logger(this)

    override fun registerChannelReceiverListener(receiver: Receiver<String>) {
        for (list in receiveListeners.values) {
            list.add(receiver)
        }
    }

    override fun registerChannelReceiveListener(port: Int, receiver: Receiver<String>) {
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

    override fun broadcastOnChannel(port: Int, message: String, vararg args: String) {
        val transceiver = getTransceiverMap()[port]
        transceiver?.broadcast(message)
    }

    override fun broadcastOnAllChannels(message: String, vararg args: String) {
        val transceiverMap = getTransceiverMap()
        for (transceiver in transceiverMap.values) {
            transceiver.broadcast(message)
        }
    }

    override fun sendMessage(addr: SocketAddress, message: String) {
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

        val transceiver = StringServerTransceiver(port)

        return if (addChannel(port, transceiver)) {
            receiveListeners[port] = ArrayList()
            true
        } else {
            false
        }
    }

    override fun createServerBootstrap(port: Int): ServerBootstrap {
        val transceiver = getTransceiverMap()[port] as StringServerTransceiver
        val channel = StringServerChannel(transceiver,
            ServerChannelInfo(
                serverConfig.USE_SSL,
                serverConfig.CHANNEL_IDS.incrementAndGet(),
                serverConfig.WRITE_IDLE_TIME))
        return createServerBootstrap(channel)
    }

    override fun connectionActive(handler: Handler<String>) {
        for (listener in connectionListeners) {
            listener.onConnection(handler.remoteAddress)
        }
    }

    override fun connectionInActive(handler: Handler<String>) {
        for (listener in disconnectionListeners) {
            listener.onDisconnection(handler.remoteAddress)
        }
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: String) {
        serverScope.launch {
            receiveMessage(addr, port, message)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: String) {
        logger.trace("{}", message)
        val receiveListenerList = receiveListeners[port]
        if (receiveListenerList != null) {
            for (listener in receiveListenerList) {
                listener.handle(addr, message)
            }
        }
    }


}
