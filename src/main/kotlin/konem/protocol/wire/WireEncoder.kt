package konem.protocol.wire

import io.netty.buffer.Unpooled.wrappedBuffer
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import konem.data.protobuf.KonemMessage

@ChannelHandler.Sharable
class WireEncoder : MessageToMessageEncoder<KonemMessage>() {
  @Throws(Exception::class)
  override fun encode(   ctx: ChannelHandlerContext,  msg: KonemMessage,  out: MutableList<Any> ) {
    out.add(wrappedBuffer(msg.encode()))
  }
}
