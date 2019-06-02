package eb.netty.stream

interface ChannelReader {

    /**
     *
     * Registers a Receiver on all active ports
     *
     *
     * WARNING -
     * Receivers registered with this method will receive all channel reads from all ports
     *
     * @param receiver
     */
    fun registerChannelReadListener(receiver: Receiver)

}
