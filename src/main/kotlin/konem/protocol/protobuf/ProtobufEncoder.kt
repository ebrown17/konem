package konem.protocol.protobuf

import com.squareup.wire.Message
import konem.data.protobuf.KonemPMessage


import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.MessageToMessageEncoder

import io.netty.buffer.Unpooled.*

/**
 * Encodes the requested [Google
 * Protocol Buffers](https://github.com/google/protobuf) [Message] and [MessageLite] into a
 * [ByteBuf]. A typical setup for TCP/IP would be:
 * <pre>
 * [ChannelPipeline] pipeline = ...;
 *
 * // Decoders
 * pipeline.addLast("frameDecoder",
 * new [LengthFieldBasedFrameDecoder](1048576, 0, 4, 0, 4));
 * pipeline.addLast("protobufDecoder",
 * new [ProtobufDecoder](MyMessage.getDefaultInstance()));
 *
 * // Encoder
 * pipeline.addLast("frameEncoder", new [LengthFieldPrepender](4));
 * pipeline.addLast("protobufEncoder", new [ProtobufEncoder]());
</pre> *
 * and then you can use a `MyMessage` instead of a [ByteBuf]
 * as a message:
 * <pre>
 * void channelRead([ChannelHandlerContext] ctx, Object msg) {
 * MyMessage req = (MyMessage) msg;
 * MyMessage res = MyMessage.newBuilder().setText(
 * "Did you say '" + req.getText() + "'?").build();
 * ch.write(res);
 * }
</pre> *
 */
@Sharable
class ProtobufEncoder : MessageToMessageEncoder<Message<KonemPMessage, KonemPMessage.Builder>>() {
  @Throws(Exception::class)
  override fun encode(
    ctx: ChannelHandlerContext,
    msg: Message<KonemPMessage, KonemPMessage.Builder>,
    out: MutableList<Any>
  ) {
    if (msg is KonemPMessage) {
      out.add(wrappedBuffer(msg.encode()))
      return
    }
    else{
      println("ProtobufEncoder XXXX $msg")
    }
   /* if (msg is KonemPMessage.) {
      out.add(wrappedBuffer(msg.newBuilder().build().encode()))
    }*/
  }
}
