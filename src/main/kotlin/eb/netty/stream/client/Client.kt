package eb.netty.stream.client

import eb.netty.stream.ChannelReader
import eb.netty.stream.ClientConnectionListener
import eb.netty.stream.ConnectionStatusListener
import eb.netty.stream.Transceiver
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.TimeUnit

abstract class Client(private val serverAddress: InetSocketAddress, config: ClientBootstrapConfig) : ChannelReader {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val transceiver: Transceiver<Any>
    protected val bootstrap: Bootstrap
    protected val clietScope: CoroutineScope

    var channel: Channel? = null

    private var retryListener: ClientConnectionListener? = null
    private var closedListener: ClientClosedConnectionListener? = null
    private val connectionListeners: MutableList<ConnectionStatusListener>

    private var retryCount = 0
    private var retryTime: Long = 0
    internal var isDisconnectInitiated = true
        private set

    init {
        transceiver = config.transceiver
        bootstrap = config.bootstrap
        clietScope = config.scope
        connectionListeners = ArrayList<ConnectionStatusListener>()
    }


    fun isActive(): Boolean {
        return channel != null && (channel!!.isOpen || channel!!.isActive)
    }


    @Throws(InterruptedException::class)
    fun connect() {
        if (isActive()) {
            logger.warn("connect called while connection already active")
            return
        }
        if (retryListener != null && retryListener!!.isAttemptingConnection) {
            logger.warn("connect called while connection attempt already in progress")
            return
        }
        if (retryListener == null) {
            logger.info("connect creating new connection listener")
            retryListener = ClientConnectionListener(this)
        }

        val channelFuture = bootstrap.connect(serverAddress)
        retryListener!!.setAttemptingConnection()
        channelFuture.addListener(retryListener)
    }

    internal fun connectionEstablished(future: ChannelFuture) {
        logger.info("connectionEstablished Client connected to {} ", serverAddress.hostString)
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
        logger.info("disconnect disconnect explicitly called")
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
        clietScope.launch {
            delay(1000)
            for (listener in connectionListeners) {
                listener.onConnection(serverAddress)
            }
        }
    }

    private fun handleDisconnection(){
        clietScope.launch {
            delay(1000)
            for (listener in connectionListeners) {
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

        if (retryTime >= RETRY_TIME * 1000L) {
            retryCount++
        }
        if (this.retryTime == 0L) {
            this.retryTime = retryTime
            retryTime = 0
        }

        if (retryCount >= MAX_RETRY_UNTIL_INCR) {
            logger.debug(
                    "calculateRetryTime {} >= {} setting {} as retry interval: total time retrying {} seconds",
                    retryCount,
                    MAX_RETRY_UNTIL_INCR,
                    MAX_RETRY_TIME,
                    retryTime / 1000L)
            return MAX_RETRY_TIME
        } else {
            logger.debug(
                    "calculateRetryTime {} < {} setting {} seconds as retry interval: total time retrying {} seconds",
                    retryCount,
                    MAX_RETRY_UNTIL_INCR,
                    RETRY_TIME,
                    retryTime / 1000L)
            return RETRY_TIME
        }
    }

    fun shutdown() {
        val channel = channel
        channel?.close()
    }

    fun registerOnConnectionListener(listener: ConnectionStatusListener) {
        connectionListeners.add(listener)
    }

    companion object {
        private val RETRY_TIME = 10L
        private val MAX_RETRY_TIME = 60L
        private val MAX_RETRY_UNTIL_INCR = 30
    }


}