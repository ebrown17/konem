package konem.protocol.konem.json

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.MessageToMessageCodec
import io.netty.handler.codec.MessageToMessageEncoder
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer

class KonemJsonDecoder: SimpleChannelInboundHandler<String>() {
    private val serializer = KonemMessageSerializer()

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: String) {
        if(msg.isNotBlank()){
            ctx?.fireChannelRead(serializer.toKonemMessage(msg))
        }
    }
}
@ChannelHandler.Sharable
class KonemJsonEncoder: MessageToMessageEncoder<KonemMessage>() {
    private val serializer = KonemMessageSerializer()

    override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: MutableList<Any>) {
        out.add(serializer.toJson(msg))
    }
}
