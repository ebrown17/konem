package konem.protocol.websocket

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.ExceptionHandler
import konem.netty.HeartbeatReceiver
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo


class WebSocketClientChannel<T>(
    val transceiver: WebSocketTransceiver<T>,
    private val clientChannelInfo: ClientChannelInfo<T>
) : ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()

        val protocolPipeline = clientChannelInfo.protocol_pipeline.getProtocolPipelineCodecs()
        val heartbeatProtocol = clientChannelInfo.heartbeatProtocol
        val handlerPair = clientChannelInfo.protocol_pipeline.getProtocolMessageHandler()
        val handlerName = handlerPair.first
        val messageHandler = handlerPair.second

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
                    heartbeatProtocol.isHeartbeat
                )
            )
        }

        pipeline.addLast(handlerName, messageHandler)
        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
