package konem.netty.stream.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import konem.netty.stream.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.ArrayList
import java.util.concurrent.TimeUnit

abstract class Client(private val serverAddress: SocketAddress, config: ClientBootstrapConfig) :
  ChannelReader {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val transceiver: Transceiver<*> = config.transceiver
  protected val bootstrap: Bootstrap = config.bootstrap
  protected val clientScope: CoroutineScope = config.scope

  var channel: Channel? = null

  private var retryListener: ClientConnectionListener? = null
  private var closedListener: ClientClosedConnectionListener? = null
  private val connectionListeners: MutableList<ConnectListener> = ArrayList()
  private val disconnectionListeners: MutableList<DisconnectListener> = ArrayList()

  var retryCount = 0
    private set
  var retryTime: Long = 0
    private set
  internal var isDisconnectInitiated = true
    private set

  fun isActive(): Boolean {
    return channel != null && (channel!!.isOpen || channel!!.isActive)
  }

  @Throws(InterruptedException::class)
  fun connect() {
    if (isActive()) {
      logger.warn("called while connection already active")
      return
    }
    if (retryListener != null && retryListener!!.isAttemptingConnection) {
      logger.warn("called while connection attempt already in progress")
      return
    }
    if (retryListener == null) {
      logger.info("creating new connection listener")
      retryListener = ClientConnectionListener(this)
    }

    val channelFuture = bootstrap.connect(serverAddress)
    retryListener!!.setAttemptingConnection()
    channelFuture.addListener(retryListener)
  }

  internal fun connectionEstablished(future: ChannelFuture) {
    logger.info("Client connected to {} ", serverAddress.toString())
    retryCount = 0
    retryTime = 0
    isDisconnectInitiated = false
    channel = future.channel()
    transceiver.registerChannelReader(serverAddress, this)
    closedListener = ClientClosedConnectionListener(this)
    channel!!.closeFuture().addListener(closedListener)
    handleConnection()
  }

  @Throws(IOException::class)
  fun disconnect() {
    logger.info("disconnect explicitly called")
    isDisconnectInitiated = true
    if (channel == null || !isActive()) {
      logger.info("disconnect called when connection not active or channel null")
      return
    }
    channel!!.closeFuture().removeListener(closedListener)
    channel!!.close().awaitUninterruptibly(1, TimeUnit.SECONDS)
    handleDisconnection()
  }

  private fun handleConnection() {
    clientScope.launch {
      delay(oneSecond)
      for (listener in connectionListeners) {
        listener.onConnection(serverAddress)
      }
    }
  }

  private fun handleDisconnection() {
    clientScope.launch {
      delay(oneSecond)
      for (listener in disconnectionListeners) {
        listener.onDisconnection(serverAddress)
      }
    }
  }

  /**
   * @return Will return the time in milliseconds. Returns the `RETRY_TIME` for the specified
   * `retryCount`. After this limit is reached it will then only return the time specified
   * with `MAX_RETRY_TIME`.
   */
  internal fun calculateRetryTime(): Long {
    var retryTime = System.currentTimeMillis() - this.retryTime

    if (retryTime >= RETRY_TIME * oneSecond) {
      retryCount++
    }
    if (this.retryTime == 0L) {
      this.retryTime = retryTime
      retryTime = 0
    }

    if (retryCount >= MAX_RETRY_UNTIL_INCR) {
      logger.debug(
        "{} >= {} setting {} as retry interval: total time retrying {} seconds",
        retryCount,
        MAX_RETRY_UNTIL_INCR,
        MAX_RETRY_TIME,
        retryTime / oneSecond
      )
      return MAX_RETRY_TIME
    } else {
      logger.debug(
        "{} < {} setting {} seconds as retry interval: total time retrying {} seconds",
        retryCount,
        MAX_RETRY_UNTIL_INCR,
        RETRY_TIME,
        retryTime / oneSecond
      )
      return RETRY_TIME
    }
  }

  fun shutdown() {
    val channel = channel
    channel?.close()
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

  companion object {
    private const val RETRY_TIME = 10L
    private const val MAX_RETRY_TIME = 60L
    private const val MAX_RETRY_UNTIL_INCR = 30
    private const val oneSecond = 1_000L
  }
}
