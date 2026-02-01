package konem.protocol.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import konem.netty.Handler
import konem.netty.ServerTransceiver
import konem.netty.Transceiver

abstract class WebSocketHandler<T>(
    val webSocketPath: String,
    handlerId: Long,
    transceiver: Transceiver<T>
)
    : Handler<T>(handlerId,transceiver) {

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
    fun getHandler(wsPath: String): WebSocketHandler<T>{
         return object: WebSocketHandler<T>(wsPath,handlerId,transceiver){
            override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                transceiverReceive(message,webSocketPath)
            }
        }
    }
}
