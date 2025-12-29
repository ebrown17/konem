package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.netty.Handler
import konem.protocol.websocket.WebSocketHandler

class KonemJsonMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message:KonemMessage ) {
        transceiverReceive(message)
    }

}
class KonemJsonWebSocketMessageHandler(webSocketPath: String) : WebSocketHandler<KonemMessage>(webSocketPath) {

    override fun channelRead0(ctx: ChannelHandlerContext, message:KonemMessage ) {
        transceiverReceive(message)
    }

}
