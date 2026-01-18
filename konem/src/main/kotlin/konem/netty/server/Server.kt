package konem.netty.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import konem.netty.BaseServerChannelReceiverRegistrant
import konem.netty.ChannelReceiver
import konem.netty.ConnectListener
import konem.netty.ConnectionListener
import konem.netty.ConnectionStatusListener
import konem.netty.DisconnectListener
import konem.netty.DisconnectionListener
import konem.netty.Handler
import konem.netty.HandlerListener
import konem.netty.ProtocolPipeline
import konem.netty.ServerHeartbeatProtocol
import konem.netty.ServerTransceiver
import konem.netty.WebSocketServerChannelReceiverRegistrant
import konem.protocol.websocket.WebSocketConnectionListener
import konem.protocol.websocket.WebSocketConnectionStatusListener
import konem.protocol.websocket.WebSocketDisconnectionListener
import konem.protocol.websocket.WsConnectListener
import konem.protocol.websocket.WsDisconnectListener
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


open class BaseConfig {
    var BOSSGROUP_NUM_THREADS: Int = 1
    var WORKGROUP_NUM_THREADS: Int = 0
    var USE_SSL: Boolean = false
    var WRITE_IDLE_TIME: Int = 10
    var CHANNEL_IDS: AtomicLong = AtomicLong(0L)
    var SO_BACKLOG: Int = 25
    var SO_KEEPALIVE: Boolean = true
    var TCP_NODELAY: Boolean = true
}

class ServerConfig : BaseConfig() {
    internal val portSet = mutableSetOf<Int>()

    /**
     *
     * @param port The port to be used for this channel
     */
    fun addChannel(port: Int) {
        portSet.add(port)
    }
}

class WebSocketServerConfig : BaseConfig() {
    internal val portToWsMap: HashMap<Int, Array<String>> = HashMap()

    /**
     *
     * @param port The port to be used for this channel
     * @param websocketPaths vararg list of websocket paths to add to this port
     */
    fun addChannel(port: Int, vararg websocketPaths: String) {
        portToWsMap.putIfAbsent(port, hashSetOf(*websocketPaths).toTypedArray())
    }

}

data class ServerChannelInfo<T>(
    val use_ssl: Boolean,
    val channel_id: Long,
/*    val maxContentLength: Int = 65536,
    val maxAllocation: Int = 1024 * 1024 * 50,*/
    val heartbeatProtocol: ServerHeartbeatProtocol,
    val protocol_pipeline: ProtocolPipeline<T>
)

interface WebSocketServer<T>: Server<T>, WebSocketServerChannelReceiverRegistrant<T> {
    /**
     * Sends a message to all connected clients on specified port
     *
     * @param port port of channel to send message on
     * @param message to send
     * @param args any extra arguments needed to specify which channels
     */
    fun broadcastOnChannel(port: Int, message: T, vararg args: String)

    /**
     * Broadcasts a message on all channels.
     *
     * @param message
     * @param args any extra arguments needed to specify which channels
     */
    fun broadcastOnAllChannels(message: T, vararg args: String)

    fun registerPathConnectionListener(listener: WebSocketConnectionListener)

    fun registerPathDisconnectionListener(listener: WebSocketDisconnectionListener)

    fun registerPathConnectionStatusListener(listener: WebSocketConnectionStatusListener)

    fun isPathConfigured(port: Int, path: String): Boolean
}

interface TcpSocketServer<T>: Server<T>{
    /**
     * Sends a message to all connected clients on specified port
     *
     * @param port port of channel to send message on
     * @param message to send
     */
    fun broadcastOnChannel(port: Int, message: T)

    /**
     * Broadcasts a message on all channels.
     *
     * @param message
     */
    fun broadcastOnAllChannels(message: T)
}


interface Server<T> : BaseServerChannelReceiverRegistrant<T> {

    /**
     * Sends a message to specified host
     *
     * @param addr
     * @param message
     */
    fun sendMessage(addr: SocketAddress, message: T)

    fun registerConnectionListener(listener: ConnectionListener)
    fun registerDisconnectionListener(listener: DisconnectionListener)
    fun registerConnectionStatusListener(listener: ConnectionStatusListener)

    @Throws(InterruptedException::class)
    fun startServer()
    fun shutdownServer()
    fun allActive(): Boolean
    fun isActive(port: Int): Boolean

}

abstract class WebSocketServerInternal<T>(
    serverConfig: WebSocketServerConfig, heartbeatProtocol: ServerHeartbeatProtocol,
    protocolPipeline: ProtocolPipeline<T>
) : ServerInternal<T>(serverConfig,heartbeatProtocol,protocolPipeline){
    internal val pathConnectionListeners: MutableList<WsConnectListener> = ArrayList()
    internal val pathDisconnectionListeners: MutableList<WsDisconnectListener> = ArrayList()
    internal val websocketMap: ConcurrentHashMap<Int, Array<String>> = ConcurrentHashMap()

    internal fun isPathConfiguredOnPort(port: Int, path: String): Boolean {
        val configuredPaths = websocketMap[port]
        return configuredPaths?.contains(path) ?: false
    }

    internal fun onPathConnect(remoteConnection: InetSocketAddress, paths: String) {
        for (listener in pathConnectionListeners) {
            listener.onConnection(remoteConnection, paths)
        }
    }

    internal fun onPathDisconnect(remoteConnection: InetSocketAddress, paths: String) {
        for (listener in pathDisconnectionListeners) {
            listener.onDisconnection(remoteConnection, paths)
        }
    }
}

abstract class ServerInternal<T>(
    val serverConfig: BaseConfig, val heartbeatProtocol: ServerHeartbeatProtocol,
    val protocolPipeline: ProtocolPipeline<T>
) : HandlerListener<T>, Server<T>, ChannelReceiver<T> {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val bossGroup: MultiThreadIoEventLoopGroup
    private val workerGroup: MultiThreadIoEventLoopGroup
    private var bootstrapMap: ConcurrentHashMap<Int, ServerBootstrap> = ConcurrentHashMap()
    private val channelMap: ConcurrentHashMap<Int, Channel> = ConcurrentHashMap()
    private val channelListenerMap: ConcurrentHashMap<Int, ArrayList<ChannelFutureListener>> =
        ConcurrentHashMap()
    private val portAddressMap: ConcurrentHashMap<Int, SocketAddress> = ConcurrentHashMap()
    private val transceiverMap: ConcurrentHashMap<Int, ServerTransceiver<T>> = ConcurrentHashMap()
    internal val channelConnectionMap: ConcurrentHashMap<Int, ArrayList<SocketAddress>> =
        ConcurrentHashMap()
    internal val remoteHostToChannelMap: ConcurrentHashMap<SocketAddress, Int> =
        ConcurrentHashMap()

    internal val connectionListeners: MutableList<ConnectListener> = ArrayList()
    internal val disconnectionListeners: MutableList<DisconnectListener> = ArrayList()

    protected val serverScope = CoroutineScope(CoroutineName("ServerScope"))

    init {
        val threadFactory = NioIoHandler.newFactory()
        bossGroup = MultiThreadIoEventLoopGroup(serverConfig.BOSSGROUP_NUM_THREADS, threadFactory)
        workerGroup = MultiThreadIoEventLoopGroup(serverConfig.WORKGROUP_NUM_THREADS, threadFactory)
    }

    protected abstract fun createServerBootstrap(port: Int): ServerBootstrap

    protected fun createServerBootstrap(channel: ChannelInitializer<SocketChannel>): ServerBootstrap {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
        bootstrap.channel(NioServerSocketChannel::class.java)
        bootstrap.option(ChannelOption.SO_BACKLOG, serverConfig.SO_BACKLOG)
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, serverConfig.SO_KEEPALIVE)
        bootstrap.childOption(ChannelOption.TCP_NODELAY, serverConfig.TCP_NODELAY)
        bootstrap.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        bootstrap.childHandler(channel)
        return bootstrap
    }

    /**
     *
     * @param port The port to be used for this channel
     * @param args Any extra arguments you may wish to use for channel configuration
     * @return true if channel successfully added
     */
    abstract fun addChannel(port: Int, vararg args: String): Boolean

    protected fun addChannel(port: Int, transceiver: ServerTransceiver<T>): Boolean {
        if (!isPortConfigured(port)) {
            if (!isTransceiverConfigured(port)) {
                if (!isBootstrapConfigured(port)) {
                    portAddressMap[port] = InetSocketAddress(port)
                    transceiverMap[port] = transceiver
                    bootstrapMap[port] = createServerBootstrap(port)
                    transceiver.registerHandlerListener(this)
                    return true
                }
            }
        }
        return false
    }

    @Throws(InterruptedException::class)
    private fun startChannel(port: Int) {
        val socketAddress = portAddressMap[port]
        if (socketAddress == null) {
            logger.error("called with non-configured port: {}", port)
            return
        }
        if (isActive(port)) {
            logger.warn("called or an already active channel: {}", port)
            return
        }
        val bootstrap = bootstrapMap[port]
        if (bootstrap != null) {
            val channelFuture = bootstrap.bind(socketAddress)
            channelFuture.await()
            if (channelFuture.isSuccess) {
                logger.debug("now listening for connections on port {}", port)
                val channel = channelFuture.channel()
                val closeListener = ChannelFutureListener { future: ChannelFuture ->
                    logger.info("Channel {} closed unexpectedly, closed with {} ", port, future.cause().message)
                    channel.close()
                }

                var listenerList = channelListenerMap[port]
                if (listenerList == null) {
                    listenerList = ArrayList<ChannelFutureListener>()
                }
                listenerList.add(closeListener)

                channel.closeFuture().addListener(closeListener)

                channelMap[port] = channel
                channelListenerMap[port] = listenerList
            } else {
                logger.error("failed to bind to port {} ", port)
            }
        }
    }

    private fun closeChannel(port: Int) {
        val channel = channelMap[port]

        if (channel == null || !isActive(channel)) {
            logger.info("called on already closed channel {}", port)
            return
        }

        channelListenerMap[port]?.forEach { listener -> channel.closeFuture().removeListener(listener) }

        channel.close().addListener { future ->
            if (!future.isSuccess) {
                logger.warn("channel {} not properly closed; error {}", port, future.cause())
            } else {
                logger.info("channel {} now closed", port)
            }
            channelMap.remove(port)
            channelListenerMap.remove(port)
            portAddressMap.remove(port)
        }
    }

    @Throws(InterruptedException::class)
    override fun startServer() {
        for (port in portAddressMap.keys) {
            startChannel(port)
        }
    }

    override fun shutdownServer() {
        logger.info("shut down server explicitly called")

        for (port in channelMap.keys) {
            closeChannel(port)
        }

        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    fun getChannelConnections(channelPort: Int): List<SocketAddress> {
        val channelConnections = channelConnectionMap[channelPort]
        return if (channelConnections == null) {
            emptyList()
        } else {
            Collections.unmodifiableList(channelConnections)
        }
    }

    fun getRemoteHostToChannelMap(): Map<SocketAddress, Int> {
        return Collections.unmodifiableMap(remoteHostToChannelMap)
    }

    override fun registerConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }

    override fun registerDisconnectionListener(listener: DisconnectionListener) {
        disconnectionListeners.add(listener)
    }

    override fun registerConnectionStatusListener(listener: ConnectionStatusListener) {
        connectionListeners.add(listener)
        disconnectionListeners.add(listener)
    }

    fun getTransceiverMap(): Map<Int, ServerTransceiver<T>> {
        return Collections.unmodifiableMap(transceiverMap)
    }

    override fun isActive(port: Int): Boolean {
        val channel = channelMap[port]
        return isActive(channel)
    }

    fun isActive(channel: Channel?): Boolean {
        return channel != null && (channel.isOpen || channel.isActive)
    }

    override fun allActive(): Boolean {
        for (channel in channelMap.values) {
            if (!isActive(channel)) {
                return false
            }
        }
        return true
    }

    fun isPortConfigured(port: Int): Boolean {
        return portAddressMap[port] != null
    }

    private fun isTransceiverConfigured(port: Int): Boolean {
        return transceiverMap[port] != null
    }

    private fun isBootstrapConfigured(port: Int): Boolean {
        return transceiverMap[port] != null
    }

    override fun registerActiveHandler(handler: Handler<T>, channelPort: Int, remoteConnection: SocketAddress) {
        var channelConnections = channelConnectionMap[channelPort]
        if (channelConnections == null) {
            channelConnections = ArrayList()
        }
        if (!channelConnections.contains(remoteConnection)) {
            channelConnections.add(remoteConnection)
            remoteHostToChannelMap[remoteConnection] = channelPort
            serverScope.launch {
                connectionActive(handler)
            }
        }
        val transceiver = transceiverMap[channelPort]
        transceiver?.registerChannelReceiver(remoteConnection, this)
        channelConnectionMap.putIfAbsent(channelPort, channelConnections)
    }

    override fun registerInActiveHandler(handler: Handler<T>, channelPort: Int, remoteConnection: SocketAddress) {
        val channelConnections = channelConnectionMap[channelPort]
        if (channelConnections != null) {
            channelConnections.remove(remoteConnection)
            remoteHostToChannelMap.remove(remoteConnection)
            channelConnectionMap.putIfAbsent(channelPort, channelConnections)
            serverScope.launch {
                connectionInActive(handler)
            }
        }
    }

    protected abstract fun connectionActive(handler: Handler<T>)
    protected abstract fun connectionInActive(handler: Handler<T>)

}
