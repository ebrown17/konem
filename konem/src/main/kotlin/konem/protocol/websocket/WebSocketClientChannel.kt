package konem.protocol.websocket

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.ExceptionHandler
import konem.netty.HeartbeatReceiver
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo
import java.net.URI


class WebSocketClientChannel<T>(
    val transceiver: WebSocketTransceiver<T>,
    private val clientChannelInfo: ClientChannelInfo<T>,
    private val webSocketPath: URI
) : ChannelInitializer<Channel>() {

    private val clientHandShaker: WebSocketClientHandshaker
        get() = WebSocketClientHandshakerFactory.newHandshaker(
            webSocketPath, WebSocketVersion.V13, null, true, DefaultHttpHeaders()
        )

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()

        val protocolPipeline = clientChannelInfo.protocol_pipeline.getProtocolPipelineCodecs()
        val wsFrameHandlers = clientChannelInfo.protocol_pipeline.getProtocolWebSocketPipelineFrameHandlers()
        val heartbeatProtocol = clientChannelInfo.heartbeatProtocol
        val webSocketMessageHandler = object: WebSocketHandler<T>(webSocketPath.path ){
            override fun channelRead0(p0: ChannelHandlerContext?, message: T) {
                transceiverReceive(message,webSocketPath)
            }
        }
        webSocketMessageHandler.handlerId = clientChannelInfo.channel_id
        webSocketMessageHandler.transceiver = transceiver

        if (clientChannelInfo.use_ssl) {
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
            } ?: run {
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }

        pipeline.addLast("httpServerCodec", HttpClientCodec())
        pipeline.addLast("httpAggregator", HttpObjectAggregator(maxContentLength))
        pipeline.addLast("compressionHandler", WebSocketClientCompressionHandler(maxAllocation))
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
        pipeline.addLast(
            "clientHandler-${webSocketPath.path}",
            WebSocketClientProtocolHandler(
                clientHandShaker,
                true,
                true)
        )

        wsFrameHandlers.forEach {(handlerName,handler) ->
            pipeline.addLast( handlerName, handler)
        }
        protocolPipeline.forEach { entry ->
            pipeline.addLast(entry.key, entry.value)
        }
        pipeline.addLast("messageHandler-${webSocketPath.path}", webSocketMessageHandler)
        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
    companion object {
        const val maxContentLength = 65536
        const val maxAllocation = 1024 * 1024 * 50 // 50 mb, make part of serverChannelInfo
    }
}
