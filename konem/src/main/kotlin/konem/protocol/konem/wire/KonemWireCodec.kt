package konem.protocol.konem.wire

import com.squareup.wire.ProtoAdapter
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.MessageToMessageEncoder
import konem.data.protobuf.KonemMessage

@ChannelHandler.Sharable
class KonemWireEncoder: MessageToMessageEncoder<KonemMessage>() {

    override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: MutableList<Any>) {
        val encoded = msg.encode()
        val buf = ctx.alloc().buffer(encoded.size)
        buf.writeBytes(encoded)
        out.add(buf)
    }
}

class KonemWireDecoder : SimpleChannelInboundHandler<ByteBuf>(){
    private val adapter: ProtoAdapter<KonemMessage> = KonemMessage.ADAPTER
    override fun channelRead0(ctx: ChannelHandlerContext?, buf: ByteBuf) {
        val bytes = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), bytes)
        ctx?.fireChannelRead(adapter.decode(bytes))
    }
}
