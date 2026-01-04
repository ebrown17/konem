package konem.protocol.websocket

import io.netty.channel.ChannelHandlerContext
import konem.netty.Handler
import konem.netty.ServerTransceiver

abstract class WebSocketHandler<T>(val webSocketPath: String) : Handler<T>()

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

