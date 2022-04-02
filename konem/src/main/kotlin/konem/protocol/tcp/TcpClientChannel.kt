package konem.protocol.tcp

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.ExceptionHandler
import konem.netty.HeartbeatReceiver
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo
import konem.protocol.konem.json.KonemJsonMessageHandler


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


        clientChannelInfo.protocol_pipeline.getProtocolMessageHandler().forEach { entry ->
            val handler = entry.value
            handler.handlerId = clientChannelInfo.channel_id
            handler.transceiver=transceiver

            pipeline.addLast(entry.key,handler)
        }

        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
