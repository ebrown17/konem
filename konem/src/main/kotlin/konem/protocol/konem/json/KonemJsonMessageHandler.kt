package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.netty.Handler

class KonemJsonMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message:KonemMessage ) {
        transceiverReceive(message)
    }

}
