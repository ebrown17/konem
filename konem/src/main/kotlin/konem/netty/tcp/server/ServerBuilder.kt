package konem.netty.tcp.server

import io.netty.buffer.PooledByteBufAllocator
import java.util.concurrent.atomic.AtomicLong


class ServerConfig {
    var BOSSGROUP_NUM_THREADS = 1
    var WORKGROUP_NUM_THREADS = 0
    var USE_SSL = true
    var WRITE_IDLE_TIME = 10
    var CHANNEL_IDS = AtomicLong(0L)
    var SO_BACKLOG = 25
    var SO_KEEPALIVE = true
    var TCP_NODELAY = true
    var ALLOCATOR = PooledByteBufAllocator.DEFAULT

}

data class ServerChannelInfo(val useSSL:Boolean, val channelId : Long, val write_idle_time : Int)


abstract class ServerBuilder<I> {

    abstract fun createServer(serverConfig: ServerConfig = ServerConfig()): Server<I>

}
