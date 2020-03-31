package konem.netty.stream.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultThreadFactory
import java.net.InetSocketAddress
import konem.netty.stream.Transceiver
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

abstract class ClientFactory<T,H> {

  protected val workerGroup: EventLoopGroup
  private val channelClass: Class<out Channel>
  private val allocator: PooledByteBufAllocator
  private val clientScope: CoroutineScope

  init {
    this.workerGroup = NioEventLoopGroup(DEFAULT_NUM_THREADS, DefaultThreadFactory("client", true))
    this.channelClass = NioSocketChannel::class.java
    this.allocator = PooledByteBufAllocator.DEFAULT
    this.clientScope = CoroutineScope(CoroutineName("ClientScope"))
  }

  abstract fun createClient(host: String, port: Int, vararg args: String): Client<T,H>

  private fun createBootStrap(): Bootstrap {
    val bootstrap = Bootstrap()
    bootstrap.group(workerGroup)
    bootstrap.channel(channelClass)
    bootstrap.option(ChannelOption.TCP_NODELAY, true)
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.option<ByteBufAllocator>(ChannelOption.ALLOCATOR, allocator)
    return bootstrap
  }

  protected fun createClientConfig(transceiver: Transceiver<T,H>): ClientBootstrapConfig<T,H> {
    return ClientBootstrapConfig(transceiver, createBootStrap(), clientScope)
  }

  protected abstract fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig<T,H>,
    vararg args: String
  ): Client<T,H>

  abstract fun shutdown()

  companion object {
    private const val DEFAULT_NUM_THREADS = 0
  }
}
