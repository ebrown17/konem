package konem.protocol.string

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.ExceptionHandler
import konem.netty.SslContextManager
import konem.netty.server.ServerChannelInfo

class StringServerChannel(
    private val transceiver: StringServerTransceiver,
    private val serverChannelInfo: ServerChannelInfo<String>
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
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
        //pipeline.addLast("heartBeatHandler", StringHeartbeatProducer(transceiver))


        /// add enable heartbeat boolean to channel config

        pipeline.addLast("messageHandler", StringMessageHandler(serverChannelInfo.channel_id, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
