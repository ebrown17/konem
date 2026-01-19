package konem.protocol.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import konem.netty.Handler
import konem.netty.ServerTransceiver

abstract class WebSocketHandler<T>(val webSocketPath: String) : Handler<T>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        initializeContext(ctx)
        ctx.fireChannelActive()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        when (evt) {
            WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE,
            WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE,
            is WebSocketServerProtocolHandler.HandshakeComplete -> activateHandler()
        }
        ctx.fireUserEventTriggered(evt)
    }
}

class WebSocketHandlerHolder<T>(
    val handlerId: Long,
    val transceiver: ServerTransceiver<T>
)
{
    private lateinit var webSocketMessageHandler: WebSocketHandler<T>

    fun getHandler(wsPath: String): WebSocketHandler<T>{
         webSocketMessageHandler = object: WebSocketHandler<T>(wsPath){
            override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                transceiverReceive(message,webSocketPath)
            }
        }
        webSocketMessageHandler.handlerId = handlerId
        webSocketMessageHandler.transceiver = transceiver
        return webSocketMessageHandler
    }
}
