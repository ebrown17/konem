package konem.protocol.konem.wire

import com.squareup.wire.ProtoAdapter
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageEncoder
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import java.rmi.UnexpectedException

class WebSocketFrameWireDecoder : SimpleChannelInboundHandler<WebSocketFrame>() {

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        frame: WebSocketFrame
    ) {
        when(frame) {
            is BinaryWebSocketFrame -> {
                ctx.fireChannelRead(frame.content())
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
class WebSocketFrameWireEncoder : MessageToMessageEncoder<ByteBuf>(){
    override fun encode(
        ctx: ChannelHandlerContext,
        byteBuf: ByteBuf,
        out: MutableList<Any>
    ) {
        out.add(BinaryWebSocketFrame(byteBuf))
    }

}
