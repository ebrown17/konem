package konem.protocol.socket.string


import konem.netty.tcp.ChannelReceiver
import konem.netty.tcp.ServerChannelReceiver
import java.net.SocketAddress

interface StringChannelReceiver {
    fun handleReceivedMessage(addr: SocketAddress, port: Int, message: String)

    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: String)
}

