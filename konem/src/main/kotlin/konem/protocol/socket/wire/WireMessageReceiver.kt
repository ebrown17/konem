package konem.protocol.socket.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.SocketAddress

open class WireMessageReceiver(private val receive: (SocketAddress, KonemMessage) -> Unit) :
    ReceiverHandler<Any>() {
    private val logger = LoggerFactory.getLogger(WireMessageReceiver::class.java)

    // TODO look at using channels to pass value from receiver
    override fun read(addr: SocketAddress, message: Any) {
        synchronized(this) {
            when (message) {
                is KonemMessage -> receive(addr, message)
                else -> {
                    logger.error("got unexpected message type: {} ", message.javaClass)
                }
            }
        }
    }
}
