package konem.protocol.socket.string


import konem.netty.tcp.ChannelReceiver
import konem.netty.tcp.ServerChannelReceiver
import java.net.SocketAddress

interface StringChannelReceiver<I> {
    fun handleChannelRead(addr: SocketAddress, port: Int, message: I)

    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: I)
}

interface StringClientChannelReceiver<I> : ChannelReceiver<I>, StringChannelReceiver<I>

interface JsonServerChannelReader<I> : ServerChannelReceiver<I>, StringChannelReceiver<I>
