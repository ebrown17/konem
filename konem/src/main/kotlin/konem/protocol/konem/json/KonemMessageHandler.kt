package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.netty.tcp.Handler
import konem.netty.tcp.Transceiver

/*
    Passes messages read from channel to transceiver
 */
class KonemMessageHandler(
    handlerId: Long,
    val transceiver: Transceiver<KonemMessage>
) : Handler<KonemMessage>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: KonemMessage) {
        logger.info("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
