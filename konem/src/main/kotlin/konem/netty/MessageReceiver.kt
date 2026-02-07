package konem.netty

import java.net.SocketAddress

open class MessageReceiver<T>(private val received: (ConnectionKey, T) -> Unit){

    fun handle(connectionKey: ConnectionKey, message: T) {
        receive(connectionKey, message)
    }

    /**
     * @param connectionKey from where message originated
     * @param message
     */
    open fun receive(connectionKey: ConnectionKey, message: T) {
        synchronized(this) {
            received(connectionKey, message)
        }
    }
}
