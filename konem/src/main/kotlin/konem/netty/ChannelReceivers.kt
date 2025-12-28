package konem.netty

import java.net.SocketAddress


interface ChannelReceiver<T> {
    fun handleReceivedMessage(addr: SocketAddress, port: Int, message: T, extra: String  = "")
    suspend fun receiveMessage(addr: SocketAddress, port: Int, message: T, extra: String  = "")
}

interface BaseChannelReceiverRegistrant<T> {
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
    fun registerChannelMessageReceiver(receiver: MessageReceiver<T>)
}

interface BaseServerChannelReceiverRegistrant<T> : BaseChannelReceiverRegistrant<T> {
    /**
     *
     * Registers a Receiver on specific port
     *
     * @param port port to listen on
     * @param receiver receiver to handle read data
     */
    fun registerChannelMessageReceiver(port: Int, receiver: MessageReceiver<T>)
}

interface WebSocketChannelReceiverRegistrant<T> {
    /**
     * Registers a receiver on the specified websocket paths.
     *
     * @param receiver receiver to handle read data
     * @param webSocketPaths webSocket paths you want to read
     */
    fun registerChannelMessageReceiver(receiver: MessageReceiver<T>, vararg webSocketPaths: String)
}

interface WebSocketServerChannelReceiverRegistrant<T> : WebSocketChannelReceiverRegistrant<T>{

    /**
     * Registers a receiver on the specified websocket paths for specific port.
     * @param port port to listen on
     * @param receiver receiver to handle read data
     * @param webSocketPaths webSocket paths you want to read
     */
    fun registerChannelMessageReceiver(port: Int, receiver: MessageReceiver<T>, vararg webSocketPaths: String)

}
