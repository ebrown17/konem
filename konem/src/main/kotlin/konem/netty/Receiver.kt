package konem.netty

import java.net.SocketAddress

abstract class Receiver<I> {

    fun handle(addr: SocketAddress, message: I) {
        receive(addr, message)
    }

    /**
     * If receives from multiple sources will be received, this method should be synchronized
     *
     * @param addr address from where message originated
     * @param message
     */
    abstract fun receive(addr: SocketAddress, message: I)
}


interface ChannelReceiver<I> {

    fun handleReceivedMessage(addr: SocketAddress, port: Int, message: I)

    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: I)

    /**
     *
     * Registers a Receiver on all active ports
     *
     *
     * WARNING -
     * Server receivers registered with this method will receive all channel reads from all ports
     *
     * @param receiver receiver to handle read data
     */
    fun registerChannelReceiveListener(receiver: Receiver<I>)
}

interface ServerChannelReceiver<I> : ChannelReceiver<I> {
    /**
     *
     * Registers a Receiver on specific port
     *
     * @param port port to listen on
     * @param receiver receiver to handle read data
     */
    fun registerChannelReceiveListener(port: Int, receiver: Receiver<I>)
}
