package konem.protocol.socket.string

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.tcp.ExceptionHandler
import konem.netty.tcp.SslContextManager
import konem.netty.tcp.client.ClientChannelInfo


class StringClientChannel(private val transceiver: StringTransceiver, private val clientChannelInfo: ClientChannelInfo) :
    ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if(clientChannelInfo.useSSL){
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
                println("HERE 1")
            }?: run {
                println("HERE 2")
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }

        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))

        pipeline.addLast("idleStateHandler", IdleStateHandler(clientChannelInfo.read_idle_time, 0, 0))
      //  pipeline.addLast("heartBeatHandler", WireHeartbeatReceiver(clientChannelInfo.read_idle_time, clientChannelInfo.heartbeat_miss_limit))



        pipeline.addLast("messageHandler", StringMessageHandler(clientChannelInfo.channelId, transceiver))


        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
