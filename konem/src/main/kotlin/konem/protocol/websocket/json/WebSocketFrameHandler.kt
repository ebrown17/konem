package konem.protocol.websocket.json

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class WebSocketFrameHandler(
    handlerId: Long,
    val transceiver: WebSocketTransceiver,
    val webSocketPath: String
) : Handler<WebSocketFrame, WebSocketFrame>(handlerId, transceiver) {

    private val logger = LoggerFactory.getLogger(WebSocketFrameHandler::class.java)
    private val serializer = KonemMessageSerializer()

    override fun channelRead0(ctx: ChannelHandlerContext?, frame: WebSocketFrame?) {
        logger.trace("channelRead0 {} sent: {}", remoteAddress, frame.toString())

        when (frame) {
            is TextWebSocketFrame -> {
                val kMessage = serializer.toKonemMessage(frame.text())
                transceiver.handleMessage(remoteAddress, webSocketPath, kMessage)
            }
            is BinaryWebSocketFrame -> {
                val buffer = frame.content()
                if (!buffer.isReadable) {
                    return
                }

                val message = buffer.toString(0, buffer.readableBytes(), CharsetUtil.UTF_8)
                val kMessage = serializer.toKonemMessage(message)
                transceiver.handleMessage(remoteAddress, webSocketPath, kMessage)
            }
            // Pongs will never reach this as they are dropped in the WebSocketServerProtocolHandler.
            // If you want to see Pongs, you will have to create the WebSocketServerProtocolHandler using the correct constructor.
            is PongWebSocketFrame -> logger.info("PongWebSocketFrame from {}", remoteAddress)
            else -> {
                val message = "unsupported frame type: " + frame!!.javaClass
                ctx?.fireExceptionCaught(UnsupportedOperationException(message))
                frame.release()
            }
        }
    }
}
