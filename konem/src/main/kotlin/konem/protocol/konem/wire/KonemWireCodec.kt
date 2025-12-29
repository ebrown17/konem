package konem.protocol.konem.wire

import com.squareup.wire.ProtoAdapter
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import konem.data.protobuf.KonemMessage

class KonemWireCodec: MessageToMessageCodec<ByteBuf, KonemMessage>() {

    private val adapter: ProtoAdapter<KonemMessage> = KonemMessage.ADAPTER

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val bytes: ByteArray = if (buf.hasArray()) {
            buf.array()
        } else {
            ByteBufUtil.getBytes(
                buf,
                buf.readerIndex(),
                buf.readableBytes(),
                false
            )
        }

        out.add(adapter.decode(bytes))
    }

    override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: MutableList<Any>) {
        val encoded = msg.encode()
        val buf = ctx.alloc().buffer(encoded.size)
        buf.writeBytes(encoded)
        out.add(buf)
    }
}
