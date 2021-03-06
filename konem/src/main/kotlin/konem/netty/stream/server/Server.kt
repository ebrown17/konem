package konem.netty.stream.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import konem.netty.stream.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

abstract class Server<T, H> : ChannelReader, HandlerListener<H, T> {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val bossGroup: NioEventLoopGroup
  private val workerGroup: NioEventLoopGroup
  private var bootstrapMap: ConcurrentHashMap<Int, ServerBootstrap> = ConcurrentHashMap()
  private val channelMap: ConcurrentHashMap<Int, Channel> = ConcurrentHashMap()
  private val channelListenerMap: ConcurrentHashMap<Int, ArrayList<ChannelFutureListener>> =
    ConcurrentHashMap()
  private val portAddressMap: ConcurrentHashMap<Int, SocketAddress> = ConcurrentHashMap()
  private val transceiverMap: ConcurrentHashMap<Int, Transceiver<T, H>> = ConcurrentHashMap()
  internal val channelConnectionMap: ConcurrentHashMap<Int, ArrayList<SocketAddress>> =
    ConcurrentHashMap()
  internal val remoteHostToChannelMap: ConcurrentHashMap<SocketAddress, Int> =
    ConcurrentHashMap()

  internal val connectionListeners: MutableList<ConnectListener> = ArrayList()
  internal val disconnectionListeners: MutableList<DisconnectListener> = ArrayList()

  protected val serverScope = CoroutineScope(CoroutineName("ServerScope"))

  companion object {
    private const val soBacklog = 25
    private const val oneSecond = 1_000L
    private const val twoSeconds = 2_000L
  }

  init {
    val threadFactory = DefaultThreadFactory("server")
    bossGroup = NioEventLoopGroup(1, threadFactory)
    workerGroup = NioEventLoopGroup(0, threadFactory)
  }

  /**
   *
   * @param port The port to be used for this channel
   * @param args Any extra arguments you may wish to use for channel configuration
   * @return true if channel successfully added
   */
  abstract fun addChannel(port: Int, vararg args: String): Boolean

  protected abstract fun createServerBootstrap(port: Int): ServerBootstrap

  protected fun createServerBootstrap(channel: ChannelInitializer<SocketChannel>): ServerBootstrap {
    val bootstrap = ServerBootstrap()
    bootstrap.group(bossGroup, workerGroup)
    bootstrap.channel(NioServerSocketChannel::class.java)
    bootstrap.option(ChannelOption.SO_BACKLOG, soBacklog)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    bootstrap.childOption<ByteBufAllocator>(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
    bootstrap.childHandler(channel)
    return bootstrap
  }

  protected fun addChannel(port: Int, transceiver: Transceiver<T, H>): Boolean {
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
      logger.error("called with unconfigured port: {}", port)
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
          logger.info("Channel {} closed unexpectedly, closed with {}", port, future.cause())
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
  fun startServer() {
    for (port in portAddressMap.keys) {
      startChannel(port)
    }
  }

  fun shutdownServer() {
    logger.info("explicitly called; shutting down server")

    for (port in channelMap.keys) {
      closeChannel(port)
    }

    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
    logger.info("server fully shutdown")
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

  fun registerConnectionListener(listener: ConnectionListener) {
    connectionListeners.add(listener)
  }

  fun registerDisconnectionListener(listener: DisconnectionListener) {
    disconnectionListeners.add(listener)
  }

  fun registerConnectionStatusListener(listener: ConnectionStatusListener) {
    connectionListeners.add(listener)
    disconnectionListeners.add(listener)
  }

  fun getTransceiverMap(): Map<Int, Transceiver<T, H>> {
    return Collections.unmodifiableMap(transceiverMap)
  }

  fun isActive(port: Int): Boolean {
    val channel = channelMap[port]
    return isActive(channel)
  }

  fun isActive(channel: Channel?): Boolean {
    return channel != null && (channel.isOpen || channel.isActive)
  }

  fun allActive(): Boolean {
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

  override fun registerActiveHandler(handler: Handler<H, T>, channelPort: Int, remoteConnection: SocketAddress) {
    var channelConnections = channelConnectionMap[channelPort]
    if (channelConnections == null) {
      channelConnections = ArrayList()
    }
    if (!channelConnections.contains(remoteConnection)) {
      channelConnections.add(remoteConnection)
      remoteHostToChannelMap[remoteConnection] = channelPort
      serverScope.launch {
        delay(1000L)
        connectionActive(handler)
      }
    }
    val transceiver = transceiverMap[channelPort]
    transceiver?.registerChannelReader(remoteConnection, this)
    channelConnectionMap.putIfAbsent(channelPort, channelConnections)
  }

  override fun registerInActiveHandler(handler: Handler<H, T>, channelPort: Int, remoteConnection: SocketAddress) {
    val channelConnections = channelConnectionMap[channelPort]
    if (channelConnections != null) {
      channelConnections.remove(remoteConnection)
      remoteHostToChannelMap.remove(remoteConnection)
      channelConnectionMap.putIfAbsent(channelPort, channelConnections)
      serverScope.launch {
        delay(1000L)
        connectionInActive(handler)
      }
    }
  }

  protected abstract fun connectionActive(handler: Handler<H, T>)
  protected abstract fun connectionInActive(handler: Handler<H, T>)
}
