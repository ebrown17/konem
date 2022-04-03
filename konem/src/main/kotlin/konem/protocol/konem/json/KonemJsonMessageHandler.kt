package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.netty.Handler


class KonemJsonMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message:KonemMessage ) {
        logger.trace("Id=$handlerId from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(Id=$handlerId,transceiver=$transceiver)"
    }
}
