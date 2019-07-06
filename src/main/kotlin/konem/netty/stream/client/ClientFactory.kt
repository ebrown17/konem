package konem.netty.stream.client

import konem.netty.stream.Transceiver
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import java.net.InetSocketAddress

abstract class ClientFactory {

  private val workerGroup: EventLoopGroup
  private val channelClass: Class<out Channel>
  private val allocator: PooledByteBufAllocator
  internal val clientScope: CoroutineScope

  init {
    this.workerGroup = NioEventLoopGroup(DEFAULT_NUM_THREADS, DefaultThreadFactory("client", true))
    this.channelClass = NioSocketChannel::class.java
    this.allocator = PooledByteBufAllocator.DEFAULT
    this.clientScope = CoroutineScope(CoroutineName("ConnectionStatus"))
  }

  abstract fun createClient(host: String, port: Int, vararg args: String): Client

  private fun createBootStrap(): Bootstrap {
    val bootstrap = Bootstrap()
    bootstrap.group(workerGroup)
    bootstrap.channel(channelClass)
    bootstrap.option(ChannelOption.TCP_NODELAY, true)
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.option<ByteBufAllocator>(ChannelOption.ALLOCATOR, allocator)
    return bootstrap
  }

  protected fun createClientConfig(transceiver: Transceiver<Any>): ClientBootstrapConfig {
    return ClientBootstrapConfig(transceiver, createBootStrap(), clientScope)
  }

  protected abstract fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig,
    vararg args: String
  ): Client

  abstract fun shutdown()

  companion object {
    private const val DEFAULT_NUM_THREADS = 0
  }
}
