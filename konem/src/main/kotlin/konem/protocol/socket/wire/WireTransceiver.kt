package konem.protocol.socket.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.SocketAddress

class WireTransceiver(channelPort: Int) : Transceiver<KonemMessage, KonemMessage>(channelPort) {
    private val logger = LoggerFactory.getLogger(WireTransceiver::class.java)

    fun handleMessage(addr: SocketAddress, message: KonemMessage) {
        logger.trace("from {} with {}", addr, message)
        val reader = channelReaders[addr] as WireChannelReader
        reader.handleChannelRead(addr, channelPort, message)
    }

    /**
     * Broadcast a message on all channels connected to this port's transceiver.
     * @param message
     */
    fun broadcastMessage(message: KonemMessage) {
        logger.debug("message: {}", message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(message)
            }
        }
    }
}
