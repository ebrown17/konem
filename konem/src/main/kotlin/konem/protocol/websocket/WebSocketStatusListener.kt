package konem.protocol.websocket

import konem.netty.ConnectionKey
import konem.netty.StatusListener

interface WsConnectListener : StatusListener {
    fun onConnection(connectionKey: ConnectionKey, path: String)
}

interface WsDisconnectListener : StatusListener {
    fun onDisconnection(connectionKey: ConnectionKey, path: String)
}

class WebSocketConnectionListener(private val connected: (connectionKey: ConnectionKey, wsPath: String) -> Unit) : WsConnectListener {
    override fun onConnection(connectionKey: ConnectionKey, path: String) {
        synchronized(this) {
            connected(connectionKey, path)
        }
    }
}

class WebSocketDisconnectionListener(private val disconnected: (connectionKey: ConnectionKey, wsPath: String) -> Unit) : WsDisconnectListener {
    override fun onDisconnection(connectionKey: ConnectionKey, path: String) {
        synchronized(this) {
            disconnected(connectionKey, path)
        }
    }
}

class WebSocketConnectionStatusListener(
    private val connected: (connectionKey: ConnectionKey, wsPath: String) -> Unit,
    private val disconnected: (connectionKey: ConnectionKey, wsPath: String) -> Unit
) : WsConnectListener, WsDisconnectListener {

    override fun onConnection(connectionKey: ConnectionKey, path: String) {
        synchronized(this) {
            connected(connectionKey, path)
        }
    }

    override fun onDisconnection(connectionKey: ConnectionKey, path: String) {
        synchronized(this) {
            disconnected(connectionKey, path)
        }
    }
}
