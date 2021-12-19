package konem.netty.stream.client

interface ClientTransmitter<I> {
    /**
     * Sends a message to connected server
     *
     * @param message
     */
    fun sendMessage(message: I)
}
