package konem.protocol.konem.string

import io.netty.channel.ChannelHandlerContext
import konem.netty.Handler

class KonemStringMessageHandler : Handler<String>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
        transceiverReceive(message)
    }

}
