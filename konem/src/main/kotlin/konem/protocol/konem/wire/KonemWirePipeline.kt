package konem.protocol.konem.wire

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import konem.data.protobuf.KonemMessage
import konem.netty.Handler
import konem.netty.ProtocolPipeline

class KonemWirePipeline private constructor() {

    companion object {
        fun getKonemWirePipeline(): ProtocolPipeline<KonemMessage> {
            return ProtocolPipeline(
                protocolMessageHandler = {
                   Pair("messageHandler",KonemWireMessageHandler())
                },
                protoPipelineCodecs = { pipeline ->
                    pipeline["frameDecoder"] = ProtobufVarint32FrameDecoder()
                    pipeline["frameEncoder"] = ProtobufVarint32LengthFieldPrepender()
                    pipeline["konemCodec"] = KonemWireCodec()

                })
        }
    }

}

class KonemWireMessageHandler : Handler<KonemMessage>() {

    override fun channelRead0(ctx: ChannelHandlerContext, message: KonemMessage) {
        logger.info("from: {} received: {}", remoteAddress, message)
        transceiver.receive(remoteAddress, message)
    }

    override fun toString(): String {
        return "Handler(handlerId=$handlerId,transceiver=$transceiver)"
    }
}
