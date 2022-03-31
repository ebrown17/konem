package konem.protocol.konem.wire

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.data.protobuf.KonemMessage
import konem.netty.ExceptionHandler
import konem.netty.SslContextManager
import konem.netty.server.ServerChannelInfo
import konem.protocol.konem.KonemWireMessageHandler

class WireServerChannel(
    private val transceiver: WireServerTransceiver,
    private val serverChannelInfo: ServerChannelInfo<KonemMessage>
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {

        val pipeline = channel.pipeline()
        if (serverChannelInfo.use_ssl) {
            SslContextManager.getServerContext()?.let { context ->
                pipeline.addLast("serverSslHandler", context.newHandler(channel.alloc()))
            }?: run {
                throw Exception("SslContextManager.getServerContext() failed to initialize... closing channel")
            }
        }
        pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
        pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast("konemCodec", KonemWireCodec())
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
        //pipeline.addLast("heartBeatHandler", WireHeartbeatProducer(transceiver))


        /// add enable heartbeat boolean to channel config

        pipeline.addLast("messageHandler", KonemWireMessageHandler(serverChannelInfo.channel_id, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
