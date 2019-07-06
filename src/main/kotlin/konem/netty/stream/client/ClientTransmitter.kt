package konem.netty.stream.client

import java.net.InetSocketAddress

interface ClientTransmitter<I> {
  /**
   * Sends a message to specified host
   *
   * @param addr
   * @param message
   */
  fun sendMessage(addr: InetSocketAddress, message: I)
}
