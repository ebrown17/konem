package konem.netty.stream

import java.net.SocketAddress

interface StatusListener

interface ConnectListener : StatusListener {
    fun onConnection(address: SocketAddress)
}

interface DisconnectListener : StatusListener {
    fun onDisconnection(address: SocketAddress)
}

class ConnectionListener(private val connected: (SocketAddress) -> Unit) : ConnectListener {
    override fun onConnection(address: SocketAddress) {
        synchronized(this) {
            connected(address)
        }
    }
}

class DisconnectionListener(private val disconnected: (SocketAddress) -> Unit) : DisconnectListener {
    override fun onDisconnection(address: SocketAddress) {
        synchronized(this) {
            disconnected(address)
        }
    }
}

class ConnectionStatusListener(
    private val connected: (SocketAddress) -> Unit,
    private val disconnected: (SocketAddress) -> Unit
) : ConnectListener, DisconnectListener {

    override fun onConnection(address: SocketAddress) {
        synchronized(this) {
            connected(address)
        }
    }

    override fun onDisconnection(address: SocketAddress) {
        synchronized(this) {
            disconnected(address)
        }
    }
}
