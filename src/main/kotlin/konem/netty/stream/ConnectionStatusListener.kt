package konem.netty.stream

import java.net.InetSocketAddress

interface ConnectionStatusListener {

  /**
   *
   * @param address that connection was made to
   */
  fun onConnection(address: InetSocketAddress)

  /**
   *
   * @param address that disconnected
   */
  fun onDisconnection(address: InetSocketAddress)
}
