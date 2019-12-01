package konem.protocol.socket.json

import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader
import java.net.InetSocketAddress

interface JsonChannelReader {
  fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any)
}

interface JsonClientChannelReader : ChannelReader, JsonChannelReader

interface JsonServerChannelReader : ServerChannelReader, JsonChannelReader
