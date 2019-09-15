package konem.protocol.websocket

import konem.netty.stream.ChannelReader
import konem.netty.stream.Receiver
import konem.netty.stream.ServerChannelReader
import java.net.InetSocketAddress

interface WebSocketChannelReader {
  fun handleChannelRead(addr: InetSocketAddress, channelPort: Int, webSocketPath: String, message: Any)
  suspend fun readMessage(addr: InetSocketAddress, channelPort: Int, webSocketPath: String, message: Any)
}

interface WebSocketClientChannelReader : ChannelReader, WebSocketChannelReader {

  /**
   * Registers a receiver on the specified websocket paths.
   *
   * @param receiver receiver to handle read data
   * @param webSocketPaths webSocket paths you want to read
   */
  fun registerChannelReadListener(receiver: Receiver, vararg args: String)
}

interface WebSocketServerChannelReader : ServerChannelReader, WebSocketChannelReader {

  /**
   * Registers a receiver on the specified websocket paths.
   *
   * @param receiver receiver to handle read data
   * @param webSocketPaths webSocket paths you want to read
   */
  fun registerChannelReadListener(receiver: Receiver, vararg args: String)

  /**
   *
   * Registers a Receiver on specific port with the specified websocket paths
   *
   *
   * WARNING -
   * Receivers registered with this method will receive all channel reads from all ports
   *
   * @param receiver receiver to handle read data
   */
  fun registerChannelReadListener(port: Int, receiver: Receiver, vararg args: String)
}
