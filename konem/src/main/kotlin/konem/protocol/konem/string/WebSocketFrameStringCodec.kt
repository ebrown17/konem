package konem.protocol.konem.string

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageEncoder
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessageSerializer

class WebSocketFrameStringDecoder : SimpleChannelInboundHandler<WebSocketFrame>() {

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        frame: WebSocketFrame
    ) {
        when(frame) {
            is TextWebSocketFrame -> {
                ctx.fireChannelRead(frame.text())
            }
            is BinaryWebSocketFrame -> {
                val buffer = frame.content()
                if(buffer.isReadable){
                    val message = buffer.toString(0, buffer.readableBytes(), CharsetUtil.UTF_8)
                    ctx.fireChannelRead(message)
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
class WebSocketFrameStringEncoder : MessageToMessageEncoder<String>(){
    override fun encode(
        ctx: ChannelHandlerContext,
        msg: String,
        out: MutableList<Any>
    ) {
        val buf = ctx.alloc().buffer()
        buf.writeInt(msg.length)
        buf.writeCharSequence(msg, CharsetUtil.UTF_8)
        out.add(BinaryWebSocketFrame(buf))
    }

}
