package konem.netty.stream

import java.net.InetSocketAddress

interface ChannelReader {


  fun handleChannelRead(addr: InetSocketAddress, webSocketPath: String, message: Any)

  fun readMessage(addr: InetSocketAddress, webSocketPath: String, message: Any)


  /**
   *
   * Registers a Receiver on all active ports
   *
   *
   * WARNING -
   * Receivers registered with this method will receive all channel reads from all ports
   *
   * @param receiver
   */
  fun registerChannelReadListener(receiver: Receiver<Any>)

  /**
   * Registers a reader for the specified websocket path.
   * Any Request that comes in with specified type will be see by this reader.
   *
   * @param webSocketPaths - webSocket paths you want to read
   * @param reader        - the listener to handle read data
   */
  fun registerChannelReadListener(vararg args: String, receiver: Receiver<Any>)

}
