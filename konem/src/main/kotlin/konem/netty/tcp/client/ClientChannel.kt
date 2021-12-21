package konem.netty.tcp.client

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import java.util.concurrent.atomic.AtomicLong

abstract class ClientChannel : ChannelInitializer<Channel>() {
    companion object {
        const val READ_IDLE_TIME = 10
        const val HEARTBEAT_MISS_LIMIT = 2
        val channelIds = AtomicLong(0L)
    }
}
