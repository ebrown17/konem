package konem.protocol.konem.wire

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.tcp.ExceptionHandler
import konem.netty.tcp.SslContextManager
import konem.netty.tcp.server.ServerChannelInfo

class WireServerChannel(
    private val transceiver: WireServerTransceiver,
    private val serverChannelInfo: ServerChannelInfo
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {

        val pipeline = channel.pipeline()
        if (serverChannelInfo.useSSL) {
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
        pipeline.addLast("heartBeatHandler", WireHeartbeatProducer(transceiver))


        /// add enable heartbeat boolean to channel config

        pipeline.addLast("messageHandler", WireMessageHandler(serverChannelInfo.channelId, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
