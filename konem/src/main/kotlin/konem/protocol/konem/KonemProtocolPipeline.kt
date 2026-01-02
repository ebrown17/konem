package konem.protocol.konem

import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import konem.netty.ProtocolPipeline
import konem.protocol.konem.json.KonemJsonCodec
import konem.protocol.konem.json.KonemJsonMessageHandler
import konem.protocol.konem.json.WebSocketFrameJsonDecoder
import konem.protocol.konem.json.WebSocketFrameJsonEncoder
import konem.protocol.konem.string.KonemStringMessageHandler
import konem.protocol.konem.string.WebSocketFrameStringDecoder
import konem.protocol.konem.string.WebSocketFrameStringEncoder
import konem.protocol.konem.wire.KonemWireCodec
import konem.protocol.konem.wire.KonemWireMessageHandler
import konem.protocol.konem.wire.WebSocketFrameWireDecoder
import konem.protocol.konem.wire.WebSocketFrameWireEncoder

class KonemProtocolPipeline private constructor(){
    companion object {
        fun getKonemJsonPipeline(): ProtocolPipeline<konem.data.json.KonemMessage> {
            return ProtocolPipeline(
                protocolMessageHandler = {
                    Pair("messageHandler", KonemJsonMessageHandler())
                },
                protoPipelineCodecs = { pipeline ->
/*                    pipeline["jsonDecoder"] = JsonObjectDecoder()
                    pipeline["stringDecoder"] = StringDecoder(CharsetUtil.UTF_8)
                    pipeline["stringEncoder"] = StringEncoder(CharsetUtil.UTF_8)*/
                    pipeline["konemCodec"] = KonemJsonCodec()
                },
                wsPipelineFrameCodec = { pipeline ->
                    pipeline["webSocketFrameEncoder"] = WebSocketFrameJsonEncoder()
                    pipeline["webSocketFrameDecoder"] = WebSocketFrameJsonDecoder()
                })
        }

        fun getKonemWirePipeline(): ProtocolPipeline<konem.data.protobuf.KonemMessage> {
            return ProtocolPipeline(
                protocolMessageHandler = {
                    Pair("messageHandler", KonemWireMessageHandler())
                },
                protoPipelineCodecs = { pipeline ->
                    pipeline["frameDecoder"] = ProtobufVarint32FrameDecoder()
                    pipeline["frameEncoder"] = ProtobufVarint32LengthFieldPrepender()
                    pipeline["konemCodec"] = KonemWireCodec()

                },
                wsPipelineFrameCodec = { pipeline ->
                    pipeline["webSocketFrameDecoder"] = WebSocketFrameWireDecoder()
                    pipeline["webSocketFrameEncoder"] = WebSocketFrameWireEncoder()
                })
        }

        fun getKonemStringPipeline(): ProtocolPipeline<String> {
            return ProtocolPipeline(
                protocolMessageHandler = {
                    Pair("messageHandler", KonemStringMessageHandler())
                },
                protoPipelineCodecs = { pipeline ->
                    pipeline["stringDecoder"] = StringDecoder(CharsetUtil.UTF_8)
                    pipeline["stringEncoder"] = StringEncoder(CharsetUtil.UTF_8)
                },
                wsPipelineFrameCodec = { pipeline ->
                    pipeline["webSocketFrameDecoder"] = WebSocketFrameStringDecoder()
                    pipeline["webSocketFrameEncoder"] = WebSocketFrameStringEncoder()
                })
        }

    }
}
