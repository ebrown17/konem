package konem.protocol.protobuf

import konem.netty.stream.ChannelReader
import java.net.InetSocketAddress

interface ProtobufChannelReader : ChannelReader {

  fun handleChannelRead(addr: InetSocketAddress, message: Any)

  suspend fun readMessage(addr: InetSocketAddress, message: Any)
}
