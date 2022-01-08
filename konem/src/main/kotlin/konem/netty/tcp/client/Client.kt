package konem.netty.tcp.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import konem.logger
import konem.netty.tcp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketAddress
import java.util.*
import java.util.concurrent.TimeUnit


interface Client<I>:  ChannelReceiver<I> {
    fun connect()
    fun disconnect()
    fun shutdown()
    fun registerConnectionListener(listener: ConnectionListener)
    fun registerDisconnectionListener(listener: DisconnectionListener)
    fun registerConnectionStatusListener(listener: ConnectionStatusListener)
    /**
     * Sends a message to connected server
     *
     * @param message
     */
    fun sendMessage(message: I)
}

abstract class ClientInternal<I>(private val serverAddress: SocketAddress, private val config: ClientBootstrapConfig<I>) :
   Client<I>{

    private val logger = logger(javaClass)
    private val transceiver: Transceiver<I> = config.transceiver
    private val bootstrap: Bootstrap = config.bootstrap
    internal val clientScope: CoroutineScope = config.scope

    private var retryListener: ClientConnectionListener<I>? = null
    private var closedListener: ClientClosedConnectionListener<I>? = null

    private val connectionListeners: MutableList<ConnectListener> = ArrayList()
    private val disconnectionListeners: MutableList<DisconnectListener> = ArrayList()

    internal var channel: Channel? = null
        private set

    internal var isDisconnectInitiated = true
        private set

    fun isActive(): Boolean {
        return channel != null && (channel!!.isOpen || channel!!.isActive)
    }

    @Throws(InterruptedException::class)
    override fun connect() {
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
            retryListener = ClientConnectionListener(this, config.retryInfo) { channelFuture ->
                logger.info("Client connected to {} ", serverAddress.toString())
                isDisconnectInitiated = false
                channel = channelFuture.channel()
                transceiver.registerChannelReceiver(serverAddress, this)
                closedListener = ClientClosedConnectionListener(this) {
                    handleDisconnection()
                    connect()
                }
                channel!!.closeFuture().addListener(closedListener)
                handleConnection()
            }
        }

        val channelFuture = bootstrap.connect(serverAddress)
        //retryListener!!.isAttemptingConnection
        channelFuture.addListener(retryListener)
    }


    @Throws(IOException::class)
    override fun disconnect() {
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

    private fun handleConnection() = clientScope.launch {
        for (listener in connectionListeners) {
            listener.onConnection(serverAddress)
        }
    }

    private fun handleDisconnection() = clientScope.launch {
        for (listener in disconnectionListeners) {
            listener.onDisconnection(serverAddress)
        }
    }

    override fun shutdown() {
        channel?.close()
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

}
