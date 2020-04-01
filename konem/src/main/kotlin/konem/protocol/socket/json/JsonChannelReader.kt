package konem.protocol.socket.json

import java.net.SocketAddress
import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader

interface JsonChannelReader<T> {
  fun handleChannelRead(addr: SocketAddress, port: Int, message: T)

  suspend fun readMessage(addr: SocketAddress, port: Int, message: T)
}

interface JsonClientChannelReader<T> : ChannelReader, JsonChannelReader<T>

interface JsonServerChannelReader<T> : ServerChannelReader, JsonChannelReader<T>
