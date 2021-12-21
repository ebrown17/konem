package konem.netty.tcp.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import konem.netty.tcp.Transceiver

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import java.net.InetSocketAddress

class ClientFactoryConfig {
    var DEFAULT_NUM_THREADS = 0
    var RETRY_TIME = 10L
    var MAX_RETRY_TIME = 60L
    var MAX_RETRY_UNTIL_INCR = 30
    var USE_SSL = true
}

abstract class ClientFactory<I> constructor(val config: ClientFactoryConfig) {

    protected val workerGroup: EventLoopGroup
    private val channelClass: Class<out Channel>
    private val allocator: PooledByteBufAllocator
    private val clientScope: CoroutineScope

    init {
        this.workerGroup = NioEventLoopGroup(config.DEFAULT_NUM_THREADS, DefaultThreadFactory("client", true))
        this.channelClass = NioSocketChannel::class.java
        this.allocator = PooledByteBufAllocator.DEFAULT
        this.clientScope = CoroutineScope(CoroutineName("ClientScope"))
    }

    abstract fun createClient(host: String, port: Int, vararg args: String): Client<I>

    private fun createBootStrap(): Bootstrap {
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
        bootstrap.channel(channelClass)
        bootstrap.option(ChannelOption.TCP_NODELAY, true)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
        bootstrap.option(ChannelOption.ALLOCATOR, allocator)
        return bootstrap
    }

    protected fun createClientConfig(transceiver: Transceiver<I>): ClientBootstrapConfig<I> {
        return ClientBootstrapConfig(
            transceiver,
            createBootStrap(),
            clientScope,
            RetryInfo(
                config.RETRY_TIME,
                config.MAX_RETRY_TIME,
                config.MAX_RETRY_UNTIL_INCR,
            ),
            config.USE_SSL,
        )
    }

    protected abstract fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<I>,
        vararg args: String
    ): Client<I>

    abstract fun shutdown()

}
