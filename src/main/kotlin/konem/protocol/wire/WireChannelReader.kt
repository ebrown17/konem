package konem.protocol.wire

import konem.netty.stream.ChannelReader
import konem.netty.stream.ServerChannelReader
import java.net.InetSocketAddress

interface WireChannelReader {
  fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any)
}

interface WireClientChannelReader : ChannelReader, WireChannelReader

interface WireServerChannelReader : ServerChannelReader, WireChannelReader
