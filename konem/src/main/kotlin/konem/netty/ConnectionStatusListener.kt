package konem.netty

interface StatusListener

interface ConnectListener : StatusListener {
    fun onConnection(connectionKey: ConnectionKey)
}

interface DisconnectListener : StatusListener {
    fun onDisconnection(connectionKey: ConnectionKey)
}

open class ConnectionListener(private val connected: (ConnectionKey) -> Unit) : ConnectListener {
    override fun onConnection(connectionKey: ConnectionKey) {
        synchronized(this) {
            connected(connectionKey)
        }
    }
}

open class DisconnectionListener(private val disconnected: (ConnectionKey) -> Unit) : DisconnectListener {
    override fun onDisconnection(connectionKey: ConnectionKey) {
        synchronized(this) {
            disconnected(connectionKey)
        }
    }
}

open class ConnectionStatusListener(
    private val connected: (ConnectionKey) -> Unit,
    private val disconnected: (ConnectionKey) -> Unit
) : ConnectListener, DisconnectListener {

    override fun onConnection(connectionKey: ConnectionKey) {
        synchronized(this) {
            connected(connectionKey)
        }
    }

    override fun onDisconnection(connectionKey: ConnectionKey) {
        synchronized(this) {
            disconnected(connectionKey)
        }
    }
}
