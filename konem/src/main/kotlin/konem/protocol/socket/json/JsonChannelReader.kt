package konem.protocol.socket.json

import java.net.SocketAddress
import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader

interface JsonChannelReader {
  fun handleChannelRead(addr: SocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: SocketAddress, port: Int, message: Any)
}

interface JsonClientChannelReader : ChannelReader, JsonChannelReader

interface JsonServerChannelReader : ServerChannelReader, JsonChannelReader
