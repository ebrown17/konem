package konem.protocol.tcp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.*
import konem.netty.server.ServerChannelInfo
import konem.protocol.konem.json.KonemJsonMessageHandler

class TcpServerChannel<T>(
    private val transceiver: ServerTransceiver<T>,
    private val serverChannelInfo: ServerChannelInfo<T>
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

        serverChannelInfo.protocol_pipeline.getProtocolPipelineCodecs().forEach { entry ->
            pipeline.addLast(entry.key,entry.value)
        }

        if (serverChannelInfo.heartbeatProtocol.enabled) {
            val heartbeatProtocol = serverChannelInfo.heartbeatProtocol
            pipeline.addLast("idleStateHandler", IdleStateHandler(0, heartbeatProtocol.write_idle_time, 0))
            pipeline.addLast("heartBeatHandler", HeartbeatProducer(transceiver, heartbeatProtocol.generateHeartBeat))
        }

        val handlerPair = serverChannelInfo.protocol_pipeline.getProtocolMessageHandler()
        val handlerName = handlerPair.first
        val handler = handlerPair.second

        handler.handlerId = serverChannelInfo.channel_id
        handler.transceiver = transceiver

        pipeline.addLast(handlerName, handler)

        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
