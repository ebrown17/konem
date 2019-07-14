package konem.netty.stream

import java.net.InetSocketAddress

interface StatusListener

interface ConnectListener : StatusListener{
  fun onConnection(address: InetSocketAddress)
}

interface DisconnectListener : StatusListener{
  fun onDisconnection(address: InetSocketAddress)
}

class ConnectionListener(private val connected: (InetSocketAddress) -> Unit) : ConnectListener {
  override fun onConnection(address: InetSocketAddress) {
    connected(address)
  }
}

class DisconnectionListener(private val disconnected: (InetSocketAddress) -> Unit) :
  DisconnectListener {
  override fun onDisconnection(address: InetSocketAddress) {
    disconnected(address)
  }
}

class ConnectionStatusListener(
  private val connected: (InetSocketAddress) -> Unit,
  private val disconnected: (InetSocketAddress) -> Unit
) : ConnectListener,DisconnectListener {

  override fun onConnection(address: InetSocketAddress) {
    connected(address)
  }

  override fun onDisconnection(address: InetSocketAddress) {
    disconnected(address)
  }
}
