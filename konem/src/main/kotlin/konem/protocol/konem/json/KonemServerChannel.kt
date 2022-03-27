package konem.protocol.konem.json

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.tcp.ExceptionHandler
import konem.netty.tcp.SslContextManager
import konem.netty.tcp.server.ServerChannelInfo

class KonemServerChannel(
    private val transceiver: KonemServerTransceiver,
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
        pipeline.addLast("jsonDecoder", JsonObjectDecoder())
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("konemCodec", KonemJsonCodec())
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
        pipeline.addLast("heartBeatHandler", KonemHeartbeatProducer(transceiver))


        /// add enable heartbeat boolean to channel config

        pipeline.addLast("messageHandler", KonemMessageHandler(serverChannelInfo.channelId, transceiver))
        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
