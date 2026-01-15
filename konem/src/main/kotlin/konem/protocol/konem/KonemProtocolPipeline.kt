package konem.protocol.konem

import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.util.CharsetUtil
import konem.netty.ProtocolPipeline
import konem.protocol.konem.json.KonemJsonDecoder
import konem.protocol.konem.json.KonemJsonEncoder
import konem.protocol.konem.json.WebSocketFrameJsonDecoder
import konem.protocol.konem.json.WebSocketFrameJsonEncoder
import konem.protocol.konem.string.WebSocketFrameStringDecoder
import konem.protocol.konem.string.WebSocketFrameStringEncoder
import konem.protocol.konem.wire.KonemWireDecoder
import konem.protocol.konem.wire.KonemWireEncoder
import konem.protocol.konem.wire.WebSocketFrameWireDecoder
import konem.protocol.konem.wire.WebSocketFrameWireEncoder

class KonemProtocolPipeline private constructor(){
    companion object {
        fun getKonemJsonPipeline(): ProtocolPipeline<konem.data.json.KonemMessage> {
            return ProtocolPipeline(
                protoPipelineCodecs = { pipeline ->
                    pipeline["frameDecoder"] = LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4)
                    pipeline["stringDecoder"] = StringDecoder(CharsetUtil.UTF_8)
                    pipeline["konemDecoder"] = KonemJsonDecoder()
                    pipeline["frameEncoder"] = LengthFieldPrepender(4)
                    pipeline["stringEncoder"] = StringEncoder(CharsetUtil.UTF_8)
                    pipeline["konemEncoder"] = KonemJsonEncoder()
                },
                wsPipelineFrameCodec = { pipeline ->
                    pipeline["webSocketFrameEncoder"] = WebSocketFrameJsonEncoder()
                    pipeline["webSocketFrameDecoder"] = WebSocketFrameJsonDecoder()
                    pipeline["konemDecoder"] = KonemJsonDecoder()
                    pipeline["konemEncoder"] = KonemJsonEncoder()
                })
        }

        fun getKonemWirePipeline(): ProtocolPipeline<konem.data.protobuf.KonemMessage> {
            return ProtocolPipeline(

                protoPipelineCodecs = { pipeline ->
                    pipeline["frameDecoder"] = ProtobufVarint32FrameDecoder()
                    pipeline["frameEncoder"] = ProtobufVarint32LengthFieldPrepender()
                    pipeline["konemDecoder"] = KonemWireDecoder()
                    pipeline["konemEncoder"] = KonemWireEncoder()

                },
                wsPipelineFrameCodec = { pipeline ->
                    pipeline["webSocketFrameDecoder"] = WebSocketFrameWireDecoder()
                    pipeline["webSocketFrameEncoder"] = WebSocketFrameWireEncoder()
                    pipeline["frameDecoder"] = ProtobufVarint32FrameDecoder()
                    pipeline["frameEncoder"] = ProtobufVarint32LengthFieldPrepender()
                    pipeline["konemDecoder"] = KonemWireDecoder()
                    pipeline["konemEncoder"] = KonemWireEncoder()
                })
        }

        fun getKonemStringPipeline(): ProtocolPipeline<String> {
            return ProtocolPipeline(
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
