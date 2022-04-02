package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import konem.data.json.KonemMessage
import konem.netty.Handler
import konem.netty.ProtocolPipeline

class KonemJsonPipeline private constructor() {

    companion object {
        fun getKonemJsonPipeline(): ProtocolPipeline<KonemMessage> {
            return ProtocolPipeline(
                protocolMessageHandler = {
                    Pair("messageHandler", KonemJsonMessageHandler())
                },
                protoPipelineCodecs = { pipeline ->
                    pipeline["jsonDecoder"] = JsonObjectDecoder()
                    pipeline["stringDecoder"] = StringDecoder(CharsetUtil.UTF_8)
                    pipeline["stringEncoder"] = StringEncoder(CharsetUtil.UTF_8)
                    pipeline["konemCodec"] = KonemJsonCodec()
                })
        }
    }

}

class KonemJsonMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message: KonemMessage) {
        logger.info("handler $handlerId from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
