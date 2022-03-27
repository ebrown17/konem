package konem.protocol.string

import java.net.SocketAddress

interface StringChannelReceiver {
    fun handleReceivedMessage(addr: SocketAddress, port: Int, message: String)

    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: String)
}

