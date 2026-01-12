package konem.protocol.tcp

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.ExceptionHandler
import konem.netty.Handler
import konem.netty.HeartbeatReceiver
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo


class TcpClientChannel<T>(
    val transceiver: TcpTransceiver<T>,
    private val clientChannelInfo: ClientChannelInfo<T>
) : ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()

        val protocolPipeline = clientChannelInfo.protocol_pipeline.getProtocolPipelineCodecs()
        val heartbeatProtocol = clientChannelInfo.heartbeatProtocol

        val messageHandler = object: Handler<T>() {
            override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                transceiverReceive(message)
            }
        }
        messageHandler.handlerId = clientChannelInfo.channel_id
        messageHandler.transceiver = transceiver

        if (clientChannelInfo.use_ssl) {
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
            } ?: run {
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }

        protocolPipeline.forEach { entry ->
            pipeline.addLast(entry.key, entry.value)
        }

        if (heartbeatProtocol.enabled) {
            pipeline.addLast("idleStateHandler", IdleStateHandler(heartbeatProtocol.read_idle_time, 0, 0))
            pipeline.addLast(
                "heartBeatHandler",
                HeartbeatReceiver(
                    heartbeatProtocol.read_idle_time,
                    heartbeatProtocol.miss_limit,
                    heartbeatProtocol.dropHeartbeat,
                    heartbeatProtocol.isHeartbeat
                )
            )
        }

        pipeline.addLast("messageHandler", messageHandler)
        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
