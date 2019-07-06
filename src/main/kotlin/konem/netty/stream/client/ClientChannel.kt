package konem.netty.stream.client

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import java.util.concurrent.atomic.AtomicLong

abstract class ClientChannel : ChannelInitializer<Channel>() {
  companion object {
    protected const val READ_IDLE_TIME = 10
    protected const val HEARTBEAT_MISS_LIMIT = 2
    protected val channelIds = AtomicLong(0L)
  }
}
