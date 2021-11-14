package konem.protocol.websocket.json

import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.stream.SslContextManager
import konem.netty.stream.server.ServerChannel

class WebSocketServerChannel(private val transceiver: WebSocketTransceiver, vararg webSocketPaths: String) :
    ServerChannel() {

    private val webSocketPaths: Array<String> = arrayOf(*webSocketPaths)

    @Throws(Exception::class)
    override fun initChannel(channel: SocketChannel) {
        val pipeline = channel.pipeline()
        pipeline.addLast("serverSslHandler", SslContextManager.getServerContext().newHandler(channel.alloc()))

        pipeline.addLast("httpServerCodec", HttpServerCodec())
        pipeline.addLast("httpAggregator", HttpObjectAggregator(maxSize))
        pipeline.addLast("compressionHandler", WebSocketServerCompressionHandler())
        pipeline.addLast(
            WebSocketPathHandler::class.java.name,
            WebSocketPathHandler(transceiver, channelIds, *webSocketPaths)
        )
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, WRITE_IDLE_TIME, 0))
        pipeline.addLast("pingHandler", WebSocketPingHandler(transceiver))
        pipeline.addLast("exceptionHandler", WebSocketExceptionHandler())
    }

    companion object {
        const val maxSize = 65536
    }
}
