@file:Suppress("UNCHECKED_CAST")

package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.SocketAddress

@Suppress("UNCHECKED_CAST")
class JsonTransceiver(channelPort: Int) : Transceiver<KonemMessage, String>(channelPort) {
    private val logger = LoggerFactory.getLogger(JsonTransceiver::class.java)
    private val serializer = KonemMessageSerializer()

    fun handleMessage(addr: SocketAddress, message: KonemMessage) {
        logger.trace("from {} with {}", addr, message)
        val reader = channelReaders[addr] as JsonChannelReader<KonemMessage>
        reader.handleChannelRead(addr, channelPort, message)
    }

    /**
     * Sends a message to specified address if connected to this transceiver
     * @param addr
     * @param message
     */
    fun transmit(addr: SocketAddress, message: KonemMessage) {
        transmit(addr, serializer.toJson(message))
    }

    /**
     * Broadcast a message on all channels connected to this port's transceiver.
     * @param message
     */
    fun broadcastMessage(message: KonemMessage) {
        logger.debug("message: {}", message)
        val jsonMessage = serializer.toJson(message)
        synchronized(activeLock) {
            for (handler in activeHandlers.values) {
                handler.sendMessage(jsonMessage)
            }
        }
    }
}
