package konem.netty.stream

interface ChannelReader {

  /**
   *
   * Registers a Receiver on all active ports
   *
   *
   * WARNING -
   * Receivers registered with this method will receive all channel reads from all ports
   *
   * @param receiver receiver to handle read data
   */
  fun registerChannelReadListener(receiver: Receiver)

  /**
   * Registers a reader for the specified websocket path.
   * Any Request that comes in with specified type will be see by this reader.
   *
   * @param receiver receiver to handle read data
   * @param webSocketPaths webSocket paths you want to read
   */
  fun registerChannelReadListener(receiver: Receiver, vararg args: String)

  /**
   *
   * Registers a Receiver on all active ports
   *
   *
   * WARNING -
   * Receivers registered with this method will receive all channel reads from all ports
   *
   * @param receiver receiver to handle read data
   */
  fun registerChannelReadListener(port: Int, receiver: Receiver)
}
