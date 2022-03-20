package konem.protocol.socket.string

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.SslContextManager
import konem.netty.tcp.server.ServerChannelInfo

class StringServerChannel(
    private val transceiver: StringServerTransceiver,
    private val serverChannelInfo: ServerChannelInfo
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(channel: SocketChannel) {

        val pipeline = channel.pipeline()
        if (serverChannelInfo.useSSL) {
            pipeline.addLast("serverSslHandler", SslContextManager.getServerContext().newHandler(channel.alloc()))
        }
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        // internal konem heartbeat protocol
        pipeline.addLast("idleStateHandler", IdleStateHandler(0, serverChannelInfo.write_idle_time, 0))
      //  pipeline.addLast("heartBeatHandler", WireHeartbeatProducer(transceiver))



        pipeline.addLast("messageHandler", StringMessageHandler(serverChannelInfo.channelId, transceiver))


        pipeline.addLast("exceptionHandler", ExceptionHandler())

    }
}
