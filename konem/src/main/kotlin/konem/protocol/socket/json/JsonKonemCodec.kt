package konem.protocol.socket.json

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageCodec
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import java.nio.CharBuffer

class JsonKonemCodec : ByteToMessageCodec<KonemMessage>() {

  private val serializer = KonemMessageSerializer()

  override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: ByteBuf) {
    val message = ByteBufUtil.encodeString(
      ctx.alloc(),
      CharBuffer.wrap(serializer.toJson(msg)),
      CharsetUtil.UTF_8
    )
    try {
      out.writeBytes(message)
    } finally {
      message.release()
    }
  }

  override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
    out.add(serializer.toKonemMessage(msg.toString(CharsetUtil.UTF_8)))
  }


}
