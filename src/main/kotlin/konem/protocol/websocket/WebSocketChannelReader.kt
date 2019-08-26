package konem.protocol.websocket

import konem.netty.stream.ChannelReader
import java.net.InetSocketAddress

interface WebSocketChannelReader : ChannelReader {

  fun handleChannelRead(addr: InetSocketAddress, webSocketPath: String, message: Any)

  suspend fun readMessage(addr: InetSocketAddress, webSocketPath: String, message: Any)
}
