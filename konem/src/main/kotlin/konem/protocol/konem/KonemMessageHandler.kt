package konem.protocol.konem

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.netty.Handler
import konem.netty.Transceiver

/*
    Passes messages read from channel to transceiver
 */
class KonemJsonMessageHandler(
    handlerId: Long,
    val transceiver: Transceiver<KonemMessage>
) : Handler<KonemMessage>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: konem.data.json.KonemMessage) {
        logger.info("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}

class KonemWireMessageHandler(
    handlerId: Long,
    val transceiver: Transceiver<konem.data.protobuf.KonemMessage>
) : Handler<konem.data.protobuf.KonemMessage>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: konem.data.protobuf.KonemMessage) {
        logger.info("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
