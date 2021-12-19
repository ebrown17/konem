package konem.netty.tcp.client

import konem.netty.tcp.ConnectionListener
import konem.netty.tcp.ConnectionStatusListener
import konem.netty.tcp.DisconnectionListener

interface ClientPublic<I> {
    fun connect()
    fun disconnect()
    fun shutdown()
    fun registerConnectionListener(listener: ConnectionListener)
    fun registerDisconnectionListener(listener: DisconnectionListener)
    fun registerConnectionStatusListener(listener: ConnectionStatusListener)
    /**
     * Sends a message to connected server
     *
     * @param message
     */
    fun sendMessage(message: I)
}

