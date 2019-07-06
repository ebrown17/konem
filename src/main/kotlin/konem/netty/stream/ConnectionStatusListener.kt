package konem.netty.stream

import java.net.InetSocketAddress

interface ConnectionStatusListener {

  /**
   *
   * @param address that connection was made to
   */
  abstract fun onConnection(address: InetSocketAddress)

  /**
   *
   * @param address that disconnected
   */
  abstract fun onDisconnection(address: InetSocketAddress)
}
