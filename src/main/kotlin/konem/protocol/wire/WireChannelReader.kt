package konem.protocol.wire

import konem.netty.stream.ChannelReader
import java.net.InetSocketAddress

interface WireChannelReader : ChannelReader {

  fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any)

  suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any)
}
