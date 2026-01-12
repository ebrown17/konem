package konem.protocol.websocket.json

import konem.netty.stream.StatusListener
import java.net.SocketAddress

interface WsConnectListener : StatusListener {
    fun onConnection(address: SocketAddress, path: String)
}

interface WsDisconnectListener : StatusListener {
    fun onDisconnection(address: SocketAddress, path: String)
}

class WebSocketConnectionListener(private val connected: (remoteAddr: SocketAddress, wsPath: String) -> Unit) : WsConnectListener {
    override fun onConnection(address: SocketAddress, path: String) {
        synchronized(this) {
            connected(address, path)
        }
    }
}

class WebSocketDisconnectionListener(private val disconnected: (remoteAddr: SocketAddress, wsPath: String) -> Unit) : WsDisconnectListener {
    override fun onDisconnection(address: SocketAddress, path: String) {
        synchronized(this) {
            disconnected(address, path)
        }
    }
}

class WebSocketConnectionStatusListener(
    private val connected: (remoteAddr: SocketAddress, wsPath: String) -> Unit,
    private val disconnected: (remoteAddr: SocketAddress, wsPath: String) -> Unit
) : WsConnectListener, WsDisconnectListener {

    override fun onConnection(address: SocketAddress, path: String) {
        synchronized(this) {
            connected(address, path)
        }
    }

    override fun onDisconnection(address: SocketAddress, path: String) {
        synchronized(this) {
            disconnected(address, path)
        }
    }
}
