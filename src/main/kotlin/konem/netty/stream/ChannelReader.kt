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
   *
   * Registers a Receiver on specific port
   *
   * @param port port to listen on
   * @param receiver receiver to handle read data
   */
  fun registerChannelReadListener(port: Int, receiver: Receiver)

}
