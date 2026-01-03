package konem.protocol.konem.wire

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemMessage
import konem.netty.Handler
import konem.protocol.websocket.WebSocketHandler


class KonemWireMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message: KonemMessage) {
        transceiverReceive(message)
    }

}
