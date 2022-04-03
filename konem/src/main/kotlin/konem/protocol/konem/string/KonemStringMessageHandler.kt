package konem.protocol.konem.string

import io.netty.channel.ChannelHandlerContext
import konem.netty.Handler

class KonemStringMessageHandler : Handler<String>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
        logger.info("handler $handlerId from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
