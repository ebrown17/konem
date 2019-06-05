package eb.netty.stream.server

import java.net.InetSocketAddress

interface ServerTransmitter<I> {

    /**
     * Sends a message to all connected clients on specified port
     *
     * @param port port of channel to send message on
     * @param message to send
     * @param args any extra arguments needed to specify which channels
     */
    fun broadcastOnChannel(port: Int, message: I, vararg args: String)

    /**
     * Broadcasts a message on all channels.
     *
     * @param message
     * @param args any extra arguments needed to specify which channels
     */
    fun broadcastOnAllChannels(message: I,vararg args: String)

    /**
     * Sends a message to specified host
     *
     * @param addr
     * @param message
     */
    fun sendMessage(addr: InetSocketAddress, message: I)


}