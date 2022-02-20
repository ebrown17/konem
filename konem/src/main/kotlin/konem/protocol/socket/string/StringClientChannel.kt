package konem.protocol.socket.string

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.SslContextManager
import konem.netty.stream.client.ClientChannel
import konem.netty.tcp.client.ClientChannelInfo
import konem.protocol.socket.wire.WireDecoder
import konem.protocol.socket.wire.WireHeartbeatReceiver
import konem.protocol.socket.wire.WireMessageHandler
import konem.protocol.socket.wire.WireTransceiver


class StringClientChannel(private val transceiver: StringTransceiver, private val clientChannelInfo: ClientChannelInfo) :
    ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if(clientChannelInfo.useSSL){
            pipeline.addLast("clientSslHandler", SslContextManager.getClientContext().newHandler(channel.alloc()))
        }

        // internal konem heartbeat protocol
        pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
        pipeline.addLast("protobufDecoder", WireDecoder())
        pipeline.addLast("idleStateHandler", IdleStateHandler(clientChannelInfo.read_idle_time, 0, 0))
        pipeline.addLast("heartBeatHandler", WireHeartbeatReceiver(clientChannelInfo.read_idle_time, clientChannelInfo.heartbeat_miss_limit))


        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("messageHandler", StringMessageHandler(clientChannelInfo.channelId, transceiver))


        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
