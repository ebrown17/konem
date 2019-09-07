package konem.protocol.wire

import com.squareup.wire.ProtoAdapter
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import konem.data.protobuf.KonemMessage

class WireDecoder : MessageToMessageDecoder<ByteBuf>() {

  val adapter: ProtoAdapter<KonemMessage> = KonemMessage.ADAPTER

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
}
