package konem.protocol.socket.json

import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader
import java.net.InetSocketAddress
import java.net.SocketAddress

interface JsonChannelReader {
  fun handleChannelRead(addr: SocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: SocketAddress, port: Int, message: Any)
}

interface JsonClientChannelReader : ChannelReader, JsonChannelReader

interface JsonServerChannelReader : ServerChannelReader, JsonChannelReader
