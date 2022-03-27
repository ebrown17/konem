package konem.protocol.konem.wire

import com.squareup.wire.ProtoAdapter
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import konem.data.protobuf.KonemMessage

class KonemWireCodec: MessageToMessageCodec<ByteBuf,KonemMessage>() {

    private val adapter: ProtoAdapter<KonemMessage> = KonemMessage.ADAPTER

    override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
        val array: ByteArray
        val length = msg.readableBytes()
        array = if (msg.hasArray()) {
            msg.array()
        } else {
            ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false)
        }

        out.add(adapter.decode(array))
    }

    override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: MutableList<Any>) {
        out.add(Unpooled.wrappedBuffer(msg.encode()))
    }
}
