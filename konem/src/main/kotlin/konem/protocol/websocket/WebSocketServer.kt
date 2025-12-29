package konem.protocol.websocket

import io.netty.bootstrap.ServerBootstrap
import konem.logger
import konem.netty.*
import konem.netty.server.*
import konem.protocol.websocket.json.WebSocketConnectionListener
import konem.protocol.websocket.json.WebSocketConnectionStatusListener
import konem.protocol.websocket.json.WebSocketDisconnectionListener
import konem.protocol.websocket.json.WebSocketFrameHandler
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap


class WebSocketServerImp<T> internal constructor(
    serverConfig: WebSocketServerConfig, heartbeatProtocol: ServerHeartbeatProtocol<T>,
    protocolPipeline: ProtocolPipeline<T>
) : WebSocketServerInternal<T>(serverConfig, heartbeatProtocol, protocolPipeline), WebSocketServer<T> {

    private val receiveListenersMap: ConcurrentHashMap<
        Int,
        ConcurrentHashMap<String,ArrayList<MessageReceiver<T>>>> = ConcurrentHashMap()

    private val logger = logger(this)

    override fun broadcastOnChannel(port: Int, message: T, vararg webSocketPaths: String) {
        val transceiver = getTransceiverMap()[port]
        transceiver?.broadcast(message,*webSocketPaths)
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

        for(t in websocketPaths) {
            println("XXXXXZZZZZ $t")
        }

        println("paths to add ${arrayOf(websocketPaths)}")
        val validPaths = hashSetOf(*websocketPaths)

        for (path in websocketPaths) {
            if (isPathConfiguredOnPort(port, path)) {
                validPaths.remove(path)
            }
        }
        println("XCVadfasfd")
        if (validPaths.isNotEmpty()) {
            println("validpaths: $validPaths")
            val transceiver = WebSocketServerTransceiver<T>(port)
            websocketMap.putIfAbsent(port, validPaths.toTypedArray())
            println(websocketMap)
            if (addChannel(port, transceiver)) {
                receiveListenersMap[port] = ConcurrentHashMap()
                return true
            } else {
                websocketMap.remove(port)
            }
        }

        return false
    }

    override fun createServerBootstrap(port: Int): ServerBootstrap {
        val transceiver = getTransceiverMap()[port]!!
        val websocketPaths = websocketMap[port]
        val channel = WebSocketServerChannel(
            transceiver,
            ServerChannelInfo(
                serverConfig.USE_SSL,
                serverConfig.CHANNEL_IDS.incrementAndGet(),
                heartbeatProtocol,
                protocolPipeline
            ),
            *websocketPaths!!
        )
        return createServerBootstrap(channel)
    }

    override fun connectionActive(handler: Handler<T>) {
        val wsHandler = handler as WebSocketHandler<T>
        logger.info("connection active")
        onPathConnect(wsHandler.remoteAddress as InetSocketAddress, wsHandler.webSocketPath)
        for (listener in connectionListeners) {
            listener.onConnection(handler.remoteAddress)
        }
    }

    override fun connectionInActive(handler: Handler<T>) {
        val wsHandler = handler as WebSocketHandler<T>
        onPathDisconnect(wsHandler.remoteAddress as InetSocketAddress, wsHandler.webSocketPath)
        for (listener in disconnectionListeners) {
            listener.onDisconnection(handler.remoteAddress)
        }
    }

    override fun handleReceivedMessage(addr: SocketAddress, port: Int, message: T, webSocketPath: String) {
        serverScope.launch {
            receiveMessage(addr, port, message,webSocketPath)
        }
    }

    override suspend fun receiveMessage(addr: SocketAddress, port: Int, message: T, webSocketPath: String) {
        logger.trace("{}", message)
        val receiveListeners = receiveListenersMap[port]
        if (receiveListeners != null) {
            val receiveListenerList = receiveListeners[webSocketPath]
            if (receiveListenerList != null) {
                for (listener in receiveListenerList) {
                    listener.handle(addr, message)
                }
            }
        }
    }

    override fun registerChannelMessageReceiver(receiver: MessageReceiver<T>) {
        for( receiverListeners in receiveListenersMap.values ) {
            for(wsPaths in websocketMap.values) {
                for(path in wsPaths) {
                    var receiverListnerList: ArrayList<MessageReceiver<T>>? = receiverListeners[path]
                    if (receiverListnerList == null) {
                        receiverListnerList = ArrayList()
                    }
                    receiverListnerList.add(receiver)
                    receiverListeners[path] = receiverListnerList
                }
            }
        }
    }

    override fun registerChannelMessageReceiver(port: Int, receiver: MessageReceiver<T>) {
        if (!isPortConfigured(port)) {
            throw IllegalArgumentException("port type can't be null or port is not configured: port $port")
        }
        val receiverListeners = receiveListenersMap[port]
        if ( receiverListeners != null) {
            for(wsPaths in websocketMap.values) {
                for(path in wsPaths) {
                    var receiverListenerList: ArrayList<MessageReceiver<T>>? = receiverListeners[path]
                    if (receiverListenerList == null) {
                        receiverListenerList = ArrayList()
                    }
                    receiverListenerList.add(receiver)
                    receiverListeners[path] = receiverListenerList
                }
            }
        }
    }

    override fun registerChannelMessageReceiver(receiver: MessageReceiver<T>, vararg webSocketPaths: String) {
        require(webSocketPaths.isNotEmpty()) { "webSocketPaths type can't be null or empty" }
        for(path in webSocketPaths) {
            for(configuredPaths in websocketMap.values) {
                if(!configuredPaths.contains(path)) {
                    continue
                }
                logger.info("registering receiver for {}",path)
                for(receiverListeners in receiveListenersMap.values) {
                    var receiverListnerList: ArrayList<MessageReceiver<T>>? = receiverListeners[path]
                    if (receiverListnerList == null) {
                        receiverListnerList = ArrayList()
                    }
                    receiverListnerList.add(receiver)
                    receiverListeners[path] = receiverListnerList
                }
            }
        }
    }

    override fun registerChannelMessageReceiver(port: Int, receiver: MessageReceiver<T>, vararg webSocketPaths: String) {
        require(webSocketPaths.isNotEmpty()) { "webSocketPaths type can't be null or empty" }
        for(path in webSocketPaths) {
            for(configuredPaths in websocketMap.values) {
                if(!configuredPaths.contains(path)) {
                    continue
                }
                logger.info("registering receiver for {} on {}",path,port)
                val receiverListeners = receiveListenersMap[port]
                if(receiverListeners != null) {
                    var receiverListnerList: ArrayList<MessageReceiver<T>>? = receiverListeners[path]
                    if (receiverListnerList == null) {
                        receiverListnerList = ArrayList()
                    }
                    receiverListnerList.add(receiver)
                    receiverListeners[path] = receiverListnerList
                }
            }
        }
    }

}
