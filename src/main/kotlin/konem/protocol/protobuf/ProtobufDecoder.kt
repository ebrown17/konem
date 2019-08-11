package konem.protocol.protobuf

import com.squareup.wire.Message
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.MessageToMessageDecoder
import konem.data.protobuf.KonemPMessage

/**
 * Decodes a received [ByteBuf] into a
 * [Google Protocol Buffers](https://github.com/google/protobuf)
 * [Message] and [MessageLite]. Please note that this decoder must
 * be used with a proper [ByteToMessageDecoder] such as [ProtobufVarint32FrameDecoder]
 * or [LengthFieldBasedFrameDecoder] if you are using a stream-based
 * transport such as TCP/IP. A typical setup for TCP/IP would be:
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
class ProtobufDecoder : MessageToMessageDecoder<ByteBuf> {

  private val prototype: Message<KonemPMessage, KonemPMessage.Builder>

  constructor(prototype: Message<KonemPMessage, KonemPMessage.Builder>?) {
    if (prototype == null) {
      throw NullPointerException("prototype")
    }
    this.prototype = prototype
  }

  @Throws(Exception::class)
  override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
    val array: ByteArray
    val length = msg.readableBytes()
    array = if (msg.hasArray()) {
      msg.array()
    } else {
      ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false)
    }

    out.add(prototype.adapter().decode(array))

  }

}
