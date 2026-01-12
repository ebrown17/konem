package konem.netty

import java.net.SocketAddress

open class MessageReceiver<T>(private val received: (SocketAddress, T) -> Unit){

    fun handle(addr: SocketAddress, message: T) {
        receive(addr, message)
    }

    /**
     * @param addr address from where message originated
     * @param message
     */
    fun receive(addr: SocketAddress, message: T) {
        synchronized(this) {
            received(addr, message)
        }
    }
}
