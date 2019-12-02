package konem.protocol.socket.json

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.util.CharsetUtil


class JsonKonemMessageDecoder : ByteToMessageDecoder() {
  override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
    out.add(msg.toString(CharsetUtil.UTF_8))
  }
}
