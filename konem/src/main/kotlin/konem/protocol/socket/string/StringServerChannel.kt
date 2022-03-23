package konem.protocol.socket.string

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.tcp.ExceptionHandler
import konem.netty.tcp.SslContextManager
import konem.netty.tcp.server.ServerChannelInfo

class StringServerChannel(
    private val transceiver: StringServerTransceiver,
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
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        // internal konem heartbeat protocol
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
      //  pipeline.addLast("heartBeatHandler", WireHeartbeatProducer(transceiver))


        /// add enable heartbeat boolean to channel config

        pipeline.addLast("messageHandler", StringMessageHandler(serverChannelInfo.channelId, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
