package konem.protocol.tcp

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.ExceptionHandler
import konem.netty.HeartbeatReceiver
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo


class TcpClientChannel<T>(
    private val transceiver: TcpTransceiver<T>,
    private val clientChannelInfo: ClientChannelInfo<T>
) : ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if (clientChannelInfo.use_ssl) {
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
            } ?: run {
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }

        clientChannelInfo.protocol_pipeline.getProtocolPipelineCodecs().forEach { entry ->
            pipeline.addLast(entry.key,entry.value)
        }

        if (clientChannelInfo.heartbeatProtocol.enabled) {
            val heartbeatProtocol = clientChannelInfo.heartbeatProtocol
            pipeline.addLast("idleStateHandler", IdleStateHandler(heartbeatProtocol.read_idle_time, 0, 0))
            pipeline.addLast(
                "heartBeatHandler",
                HeartbeatReceiver<T>(
                    heartbeatProtocol.read_idle_time,
                    heartbeatProtocol.miss_limit,
                    heartbeatProtocol.isHeartbeat
                )
            )
        }


        val handlerPair = clientChannelInfo.protocol_pipeline.getProtocolMessageHandler()
        val handlerName = handlerPair.first
        val handler = handlerPair.second

        handler.handlerId = clientChannelInfo.channel_id
        handler.transceiver = transceiver

        pipeline.addLast(handlerName, handler)


        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
