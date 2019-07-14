package konem.netty.stream.server

import konem.netty.stream.ChannelReader
import konem.netty.stream.ConnectionStatusListener
import konem.netty.stream.HandlerListener
import konem.netty.stream.Transceiver
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class Server : ChannelReader, HandlerListener {

  private val logger = LoggerFactory.getLogger(javaClass)

  private val bossGroup: NioEventLoopGroup
  private val workerGroup: NioEventLoopGroup
  private var bootstrapMap: ConcurrentHashMap<Int, ServerBootstrap> = ConcurrentHashMap()
  private val channelMap: ConcurrentHashMap<Int, Channel> = ConcurrentHashMap()
  private val channelListenerMap: ConcurrentHashMap<Int, ArrayList<ChannelFutureListener>> =
    ConcurrentHashMap()
  private val portAddressMap: ConcurrentHashMap<Int, InetSocketAddress> = ConcurrentHashMap()
  private val transceiverMap: ConcurrentHashMap<Int, Transceiver<*>> = ConcurrentHashMap()
  private val channelConnectionMap: ConcurrentHashMap<Int, ArrayList<InetSocketAddress>> =
    ConcurrentHashMap()
  private val remoteHostToChannelMap: ConcurrentHashMap<InetSocketAddress, Int> =
    ConcurrentHashMap()
  private val connectionListeners: MutableList<ConnectionStatusListener> = ArrayList()

  protected val serverScope = CoroutineScope(CoroutineName("ServerScope"))

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
    bootstrap.option(ChannelOption.SO_BACKLOG, 25)
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.childOption(ChannelOption.TCP_NODELAY, true)
    bootstrap.childOption<ByteBufAllocator>(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
    bootstrap.childHandler(channel)
    return bootstrap
  }

  protected fun addChannel(port: Int, transceiver: Transceiver<*>): Boolean {
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
    } else {
      if (isActive(port)) {
        logger.warn("called or an already active channel: {}", port)
        return
      }
      val bootstrap = bootstrapMap[port]
      if (bootstrap != null) {
        val channelFuture = bootstrap.bind(socketAddress)
        channelFuture.await()
        if (channelFuture.isSuccess) {
          logger.debug("startChannel now listening for connections on port {}", port)
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
          logger.error("startChannel failed to bind to port {} ", port)
        }
      }
    }
  }

  private fun closeChannel(port: Int) {
    val channel = channelMap[port]

    if (channel == null || !isActive(channel)) {
      logger.info("closeChannel called on already closed channel {}", port)
      return
    }

    channelListenerMap[port]?.forEach { listener -> channel.closeFuture().removeListener(listener) }

    channel.close().addListener { future ->
      if (!future.isSuccess) {
        logger.warn("closeChannel channel {} error {}", port, future.cause())
      }
      channelMap.remove(port)
      channelListenerMap.remove(port)
      portAddressMap.remove(port)

      logger.info("closeChannel channel {} now closed", port)
    }
  }

  @Throws(InterruptedException::class)
  fun startServer() {
    for (port in portAddressMap.keys) {
      startChannel(port)
    }
  }

  fun shutdownServer() {
    logger.info("shutdownServer explicitly called Shutting down server ")

    for (port in channelMap.keys) {
      closeChannel(port)
    }

    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
    logger.info("shutdownServer server fully shutdown")
  }

  private fun handleConnect(remoteConnection: InetSocketAddress) {
    serverScope.launch {
      withTimeout(1500L) {
        delay(1000)
        for (listener in connectionListeners) {
          listener.onConnection(remoteConnection)
        }
      }
    }
  }

  private fun handleDisconnect(remoteConnection: InetSocketAddress) {
    serverScope.launch {
      withTimeout(1500L) {
        delay(1000)
        for (listener in connectionListeners) {
          listener.onDisconnection(remoteConnection)
        }
      }
    }
  }

  fun getChannelConnections(channelPort: Int): List<InetSocketAddress> {
    val channelConnections = channelConnectionMap[channelPort]
    return if (channelConnections == null) {
      emptyList()
    } else {
      Collections.unmodifiableList(channelConnections)
    }
  }

  fun getRemoteHostToChannelMap(): Map<InetSocketAddress, Int> {
    return Collections.unmodifiableMap(remoteHostToChannelMap)
  }

  fun registerOnConnectionListener(listener: ConnectionStatusListener) {
    connectionListeners.add(listener)
  }

  fun getTransceiverMap(): Map<Int, Transceiver<*>> {
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

  fun isTransceiverConfigured(port: Int): Boolean {
    return transceiverMap[port] != null
  }

  fun isBootstrapConfigured(port: Int): Boolean {
    return transceiverMap[port] != null
  }

  override fun registerActiveHandler(channelPort: Int, remoteConnection: InetSocketAddress) {
    var channelConnections = channelConnectionMap[channelPort]
    if (channelConnections == null) {
      channelConnections = ArrayList()
    }
    if (!channelConnections.contains(remoteConnection)) {
      channelConnections.add(remoteConnection)
      remoteHostToChannelMap[remoteConnection] = channelPort
      handleConnect(remoteConnection)
    }
    val transceiver = transceiverMap[channelPort]
    transceiver?.registerChannelReader(remoteConnection, this)
    channelConnectionMap[channelPort] = channelConnections
  }

  override fun registerInActiveHandler(channelPort: Int, remoteConnection: InetSocketAddress) {
    val channelConnections = channelConnectionMap[channelPort]
    if (channelConnections != null) {
      channelConnections.remove(remoteConnection)
      remoteHostToChannelMap.remove(remoteConnection)
      channelConnectionMap.putIfAbsent(channelPort, channelConnections)
      handleDisconnect(remoteConnection)
    }
  }
}
