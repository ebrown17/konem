package konem.protocol.socket.string


import konem.logger
import konem.netty.tcp.Transceiver
import java.net.SocketAddress


class StringTransceiver(channelPort: Int):Transceiver<String>(channelPort) {
    private val logger = logger(this)

    override fun transmit(addr: SocketAddress, message: String, vararg extra: String) {
        synchronized(activeLock) {
            logger.trace("to addr: {} with {}", addr, message)
            val handler = activeHandlers[addr]
            handler?.sendMessage(message)
        }
    }

    override fun receive(addr: SocketAddress, message: String, vararg extra: String) {
        logger.trace("from {} with {}", addr, message)
        val receiver = channelReceiver[addr] as StringChannelReceiver
        receiver.handleReceivedMessage(addr, channelPort, message)
    }
}
