package eb.netty.stream.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import java.util.concurrent.atomic.AtomicLong

abstract class ServerChannel : ChannelInitializer<SocketChannel>() {
    companion object {
        val channelIds = AtomicLong(0L)
        val WRITE_IDLE_TIME = 15
    }

}