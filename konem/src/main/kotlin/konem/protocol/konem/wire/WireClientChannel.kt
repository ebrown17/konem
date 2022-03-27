package konem.protocol.konem.wire

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.tcp.ExceptionHandler
import konem.netty.tcp.SslContextManager
import konem.netty.tcp.client.ClientChannelInfo


class WireClientChannel(private val transceiver: WireTransceiver, private val clientChannelInfo: ClientChannelInfo) :
    ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if(clientChannelInfo.useSSL){
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
            }?: run {
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }
        pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
        pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast("konemCodec", KonemWireCodec())

        pipeline.addLast("idleStateHandler", IdleStateHandler(clientChannelInfo.read_idle_time, 0, 0))
        pipeline.addLast("heartBeatHandler", WireHeartbeatReceiver(clientChannelInfo.read_idle_time, clientChannelInfo.heartbeat_miss_limit))

        pipeline.addLast("messageHandler", WireMessageHandler(clientChannelInfo.channelId, transceiver))

        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
