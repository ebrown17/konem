package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageCodec
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer

class KonemJsonCodec: MessageToMessageCodec<String,KonemMessage>() {
    private val serializer = KonemMessageSerializer()

    override fun decode(ctx: ChannelHandlerContext, msg: String, out: MutableList<Any>) {
        if(msg.isNotBlank()){
            out.add(serializer.toKonemMessage(msg))
        }
    }

    override fun encode(ctx: ChannelHandlerContext, msg: KonemMessage, out: MutableList<Any>) {
       out.add(serializer.toJson(msg))
    }
}
