package konem.protocol.socket.wire

import java.net.SocketAddress
import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader

interface WireChannelReader {
  fun handleChannelRead(addr: SocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: SocketAddress, port: Int, message: Any)
}

interface WireClientChannelReader : ChannelReader, WireChannelReader

interface WireServerChannelReader : ServerChannelReader, WireChannelReader
