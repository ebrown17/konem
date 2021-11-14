package konem.protocol.socket.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class JsonMessageHandler(
    handlerId: Long,
    val transceiver: JsonTransceiver
) : Handler<String, KonemMessage>(handlerId, transceiver) {

    private val logger = LoggerFactory.getLogger(JsonMessageHandler::class.java)
    private val serializer = KonemMessageSerializer()

    override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
        logger.trace("from: {} received: {}", remoteAddress, message)
        transceiver.handleMessage(remoteAddress, serializer.toKonemMessage(message))
    }
}
