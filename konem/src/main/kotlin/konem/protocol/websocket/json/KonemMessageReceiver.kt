package konem.protocol.websocket.json

import konem.data.json.KonemMessage
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.SocketAddress

open class KonemMessageReceiver(private val receive: (SocketAddress, KonemMessage) -> Unit) :
    ReceiverHandler<KonemMessage>() {
    private val logger = LoggerFactory.getLogger(KonemMessageReceiver::class.java)

    // TODO look at using channels to pass value from receiver

    override fun read(addr: SocketAddress, message: KonemMessage) {
        synchronized(this) {
            try {
                receive(addr, message)
            } catch (e: Exception) {
                logger.error("JsonParsingException in serializing to KonemMessage with message: {} ", e.message)
            }
        }
    }
}
