package konem.protocol.string

import io.netty.channel.ChannelHandlerContext
import konem.netty.tcp.Handler
import konem.netty.tcp.Transceiver

/*
    Passes messages read from channel to transceiver
 */
class StringMessageHandler(
    handlerId: Long,
    val transceiver: Transceiver<String>
) : Handler<String>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
        logger.trace("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
