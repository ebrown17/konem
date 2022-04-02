import konem.netty.MessageReceiver
import java.net.SocketAddress

interface ChannelReceiver<T> {

    fun handleReceivedMessage(addr: SocketAddress, port: Int, message: T)

    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: T)

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
    fun registerChannelReceiveListener(receiver: MessageReceiver<T>)
}

interface ServerChannelReceiver<T> : ChannelReceiver<T> {
    /**
     *
     * Registers a Receiver on specific port
     *
     * @param port port to listen on
     * @param receiver receiver to handle read data
     */
    fun registerChannelReceiveListener(port: Int, receiver: MessageReceiver<T>)
}
