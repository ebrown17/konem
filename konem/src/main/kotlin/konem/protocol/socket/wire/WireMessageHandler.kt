package konem.protocol.socket.wire

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class WireMessageHandler(
    handlerId: Long,
    val transceiver: WireTransceiver
) : Handler<KonemMessage, KonemMessage>(handlerId, transceiver) {

    private val logger = LoggerFactory.getLogger(WireMessageHandler::class.java)

    override fun channelRead0(ctx: ChannelHandlerContext?, message: KonemMessage?) {
        logger.info("{} sent: {}", remoteAddress, message.toString())

        when (message?.messageType) {
            MessageType.DATA -> {
                transceiver.handleMessage(remoteAddress, message)
            }
            else -> {
                ctx?.fireChannelRead(message)
            }
        }
    }
}
