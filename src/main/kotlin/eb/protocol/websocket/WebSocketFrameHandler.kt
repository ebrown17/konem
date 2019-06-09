package eb.protocol.websocket

import eb.netty.stream.Handler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory

class WebSocketFrameHandler(handlerId : Long,  transceiver : WebSocketTransceiver, val webSocketPath : String) : Handler<WebSocketFrame>(handlerId, transceiver) {

    private val logger = LoggerFactory.getLogger(WebSocketFrameHandler::class.java)

    override fun channelRead0(ctx: ChannelHandlerContext?, frame: WebSocketFrame?) {
        logger.trace("channelRead0 {} sent: {}", remoteAddress, frame.toString())
        transceiver as WebSocketTransceiver
        when (frame) {
            is TextWebSocketFrame -> {
                val message = frame.text()
                transceiver.handleMessage(remoteAddress, webSocketPath, message)
            }
            is BinaryWebSocketFrame -> {
                val buffer = frame.content()
                if (!buffer.isReadable) {
                    return
                }
                val message = buffer.toString(0, buffer.readableBytes(), CharsetUtil.UTF_8)
                transceiver.handleMessage(remoteAddress, webSocketPath, message)
            }
            // Pongs will never reach this as they are dropped in the WebSocketServerProtocolHandler.
            // If you want to see Pongs, you will have to create the WebSocketServerProtocolHandler using the correct constructor.
            is PongWebSocketFrame -> logger.info("PongWebSocketFrame from {}", remoteAddress)
            else -> {
                val message = "unsupported frame type: " + frame!!::class.java
                ctx?.fireExceptionCaught(UnsupportedOperationException(message))
            }
        }
    }
}