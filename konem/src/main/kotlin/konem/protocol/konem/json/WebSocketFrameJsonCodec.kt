package konem.protocol.konem.json

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageEncoder
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.logger
import java.rmi.UnexpectedException
import kotlin.math.log

class WebSocketFrameJsonDecoder : SimpleChannelInboundHandler<WebSocketFrame>() {
    val logger = logger(this)
    private val serializer = KonemMessageSerializer()
    override fun channelRead0(
        ctx: ChannelHandlerContext,
        frame: WebSocketFrame
    ) {
        when(frame) {
            is TextWebSocketFrame -> {
                ctx.fireChannelRead(serializer.toKonemMessage(frame.text()))
            }
            is BinaryWebSocketFrame -> {
                val buffer = frame.content()
                if(buffer.isReadable){
                    val message = buffer.toString(0, buffer.readableBytes(), CharsetUtil.UTF_8)
                    ctx.fireChannelRead(serializer.toKonemMessage(message))
                }
                else{
                    val message = "BinaryWebSocketFrame buffer not readable."
                    ctx.fireExceptionCaught(Exception(message))
                    frame.release()
                }
            }
            else -> {
                val message = "unsupported frame type: " + frame!!.javaClass
                ctx.fireExceptionCaught(UnsupportedOperationException(message))
                frame.release()
            }
        }
    }

}

@ChannelHandler.Sharable
class WebSocketFrameJsonEncoder : MessageToMessageEncoder<String>(){
    override fun encode(
        ctx: ChannelHandlerContext?,
        msg: String,
        out: MutableList<Any>
    ) {
        out.add(TextWebSocketFrame(msg))
    }
}

