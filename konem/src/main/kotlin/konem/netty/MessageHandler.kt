package konem.netty

import io.netty.channel.ChannelHandlerContext


class MessageHandler<I> (
    handlerId: Long,
    val transceiver: Transceiver<I>
) : Handler<I>(handlerId, transceiver) {

    override fun channelRead0(ctx: ChannelHandlerContext, message: I) {
        logger.info("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
