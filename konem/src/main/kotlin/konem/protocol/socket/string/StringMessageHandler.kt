package konem.protocol.socket.string

import io.netty.channel.ChannelHandlerContext
import konem.netty.tcp.Handler

/*
    Passes messages read from channel to transceiver
 */
class StringMessageHandler(
    handlerId: Long,
    val transceiver: StringTransceiver
) : Handler<String>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
        logger.trace("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }
}
