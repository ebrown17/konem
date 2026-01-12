package konem.protocol.websocket

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import konem.logger
import konem.netty.*
import konem.netty.server.ServerChannelInfo
import konem.protocol.websocket.json.WebSocketServerChannel.Companion.maxSize

class WebSocketServerChannel<T>(
    private val transceiver: ServerTransceiver<T>,
    private val serverChannelInfo: ServerChannelInfo<T>,
    vararg wsPaths: String
) : ChannelInitializer<SocketChannel>() {

    private val webSocketPaths: Array<String> = arrayOf(*wsPaths)
    private val logger = logger(this)

    override fun initChannel(channel: SocketChannel) {
        val pipeline = channel.pipeline()

        val wsFrameHandlers = serverChannelInfo.protocol_pipeline.getProtocolWebSocketPipelineFrameHandlers()
        val heartbeatProtocol = serverChannelInfo.heartbeatProtocol

        if (serverChannelInfo.use_ssl) {
            SslContextManager.getServerContext()?.let { context ->
                pipeline.addLast("serverSslHandler", context.newHandler(channel.alloc()))
            } ?: run {
                throw Exception("SslContextManager.getServerContext() failed to initialize... closing channel")
            }
        }

        pipeline.addLast("httpServerCodec", HttpServerCodec())
        pipeline.addLast("httpAggregator", HttpObjectAggregator(maxContentLength))
        pipeline.addLast("compressionHandler", WebSocketServerCompressionHandler(maxAllocation))

        val webSocketHandlerHolder = WebSocketHandlerHolder(serverChannelInfo.channel_id,transceiver)

        if (heartbeatProtocol.enabled) {
            pipeline.addLast("idleStateHandler", IdleStateHandler(0, heartbeatProtocol.write_idle_time, 0))
            pipeline.addLast("heartBeatHandler", HeartbeatProducer( heartbeatProtocol.generateHeartbeat))
        }

        pipeline.addLast(
            WebSocketPathHandler::class.java.name,
            WebSocketPathHandler(
                webSocketHandlerHolder,
                webSocketPaths
            )
        )

        wsFrameHandlers.forEach {(handlerName,handler) ->
            pipeline.addLast( handlerName, handler)
        }

        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
    companion object {
        const val maxContentLength = 65536
        const val maxAllocation = 1024 * 1024 * 50 // 50 mb, make part of serverChannelInfo
    }
}
