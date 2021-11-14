package konem.protocol.socket.json

import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader
import java.net.SocketAddress

interface JsonChannelReader<T> {
    fun handleChannelRead(addr: SocketAddress, port: Int, message: T)

    suspend fun readMessage(addr: SocketAddress, port: Int, message: T)
}

interface JsonClientChannelReader<T> : ChannelReader, JsonChannelReader<T>

interface JsonServerChannelReader<T> : ServerChannelReader, JsonChannelReader<T>
