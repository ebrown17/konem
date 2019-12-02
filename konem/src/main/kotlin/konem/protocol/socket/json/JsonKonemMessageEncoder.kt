package konem.protocol.socket.json

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.CharsetUtil
import java.nio.CharBuffer

class JsonKonemMessageEncoder : MessageToByteEncoder<String>() {

  override fun encode(ctx: ChannelHandlerContext, msg: String, out: ByteBuf) {
    val message = ByteBufUtil.encodeString(
      ctx.alloc(),
      CharBuffer.wrap(msg),
      CharsetUtil.UTF_8
    )
    try {
      out.writeBytes(message)
    } finally {
      message.release()
    }
  }

}
