package konem.protocol.websocket

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.*
import konem.netty.server.ServerChannelInfo
import konem.protocol.websocket.json.WebSocketServerChannel.Companion.maxSize

class WebSocketServerChannel<T>(
    private val transceiver: ServerTransceiver<T>,
    private val serverChannelInfo: ServerChannelInfo<T>,
    vararg webSocketPaths: String
) : ChannelInitializer<SocketChannel>() {

    private val webSocketPaths: Array<String> = arrayOf(*webSocketPaths)

    override fun initChannel(channel: SocketChannel) {
        val pipeline = channel.pipeline()

        val protocolPipeline = serverChannelInfo.protocol_pipeline.getProtocolPipelineCodecs()
        val heartbeatProtocol = serverChannelInfo.heartbeatProtocol
        val handlerPair = serverChannelInfo.protocol_pipeline.getProtocolMessageHandler()
        val handlerName = handlerPair.first
        val messageHandler = handlerPair.second

        messageHandler.handlerId = serverChannelInfo.channel_id
        messageHandler.transceiver = transceiver

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

        protocolPipeline.forEach { entry ->
            pipeline.addLast(entry.key, entry.value)
        }

        if (heartbeatProtocol.enabled) {
            pipeline.addLast("idleStateHandler", IdleStateHandler(0, heartbeatProtocol.write_idle_time, 0))
            pipeline.addLast("heartBeatHandler", HeartbeatProducer(transceiver, heartbeatProtocol.generateHeartbeat))
        }

        pipeline.addLast(handlerName, messageHandler)
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
    companion object {
        const val maxContentLength = 65536
        const val maxAllocation = 1024 * 1024 * 50 // 50 mb, make part of serverChannelInfo
    }
}
