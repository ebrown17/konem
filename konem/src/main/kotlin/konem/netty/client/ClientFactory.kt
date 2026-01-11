package konem.netty.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ProtocolPipeline
import konem.netty.Transceiver

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicLong

class ClientFactoryConfig {
    var DEFAULT_NUM_THREADS = 0
    var RETRY_TIME = 10L
    var MAX_RETRY_TIME = 60L
    var MAX_RETRY_UNTIL_INCR = 30
    var USE_SSL = false
    var channelIds = AtomicLong(0L)
}

data class ClientBootstrapConfig<T> constructor(
    val transceiver: Transceiver<T>,
    val bootstrap: Bootstrap,
    val scope: CoroutineScope,
    val retryInfo: RetryInfo,
    val clientChannelInfo: ClientChannelInfo<T>,
)

data class ClientChannelInfo<T>(
    val use_ssl: Boolean,
    val channel_id: Long,
/*    val maxContentLength: Int = 65536,
    val maxAllocation: Int = 1024 * 1024 * 50,*/
    val heartbeatProtocol: ClientHeartbeatProtocol,
    val protocol_pipeline: ProtocolPipeline<T>
)

data class RetryInfo(val retry_period: Long, val max_retry_period: Long, var retries_until_period_increase: Int)

interface ClientFactoryControl {
    fun shutdown()
}

interface TcpSocketClientFactory<T> : ClientFactoryControl {
    fun createClient(host: String, port: Int): Client<T>
    fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<T>
    ): Client<T>
}

interface WebSocketClientFactory<T> : ClientFactoryControl {
    fun createClient(host: String, port: Int, webSocketPath: String): Client<T>
    fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<T>,
        webSocketPath: String
    ): Client<T>
}


abstract class ClientFactory<T> constructor(
    private val config: ClientFactoryConfig,
    private val heartbeatProtocol: ClientHeartbeatProtocol,
    private val protocolPipeline: ProtocolPipeline<T>
) : ClientFactoryControl {

    private val workerGroup: EventLoopGroup
    private val channelClass: Class<out Channel>
    private val allocator: PooledByteBufAllocator
    private val clientScope: CoroutineScope
    internal val clientArrayList = ArrayList<Client<T>>()

    init {
        this.workerGroup = NioEventLoopGroup(config.DEFAULT_NUM_THREADS, DefaultThreadFactory("client", true))
        this.channelClass = NioSocketChannel::class.java
        this.allocator = PooledByteBufAllocator.DEFAULT
        this.clientScope = CoroutineScope(CoroutineName("ClientScope"))
    }

    private fun createBootStrap(): Bootstrap {
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
        bootstrap.channel(channelClass)
        bootstrap.option(ChannelOption.TCP_NODELAY, true)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
        bootstrap.option(ChannelOption.ALLOCATOR, allocator)
        return bootstrap
    }

    protected fun createClientConfig(transceiver: Transceiver<T>): ClientBootstrapConfig<T> {
        return ClientBootstrapConfig(
            transceiver,
            createBootStrap(),
            clientScope,
            RetryInfo(
                config.RETRY_TIME,
                config.MAX_RETRY_TIME,
                config.MAX_RETRY_UNTIL_INCR,
            ),
            ClientChannelInfo(
                config.USE_SSL,
                config.channelIds.incrementAndGet(),
                heartbeatProtocol,
                protocolPipeline
            )
        )
    }

    override fun shutdown() {
        for (client in clientArrayList) {
            client.shutdown()
        }
        clientArrayList.clear()
        workerGroup.shutdownGracefully()
    }

}
