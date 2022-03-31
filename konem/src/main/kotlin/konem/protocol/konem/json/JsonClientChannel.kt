package konem.protocol.konem.json

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.ExceptionHandler
import konem.netty.SslContextManager
import konem.netty.client.ClientChannelInfo
import konem.protocol.konem.KonemJsonMessageHandler


class JsonClientChannel(private val transceiver: JsonTransceiver, private val clientChannelInfo: ClientChannelInfo) :
    ChannelInitializer<Channel>() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        if(clientChannelInfo.useSSL){
            SslContextManager.getClientContext()?.let { context ->
                pipeline.addLast("clientSslHandler", context.newHandler(channel.alloc()))
            }?: run {
                throw Exception("SslContextManager.getClientContext() failed to initialize... closing channel")
            }
        }
        pipeline.addLast("jsonDecoder", JsonObjectDecoder())
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("konemCodec", KonemJsonCodec())

        pipeline.addLast("idleStateHandler", IdleStateHandler(clientChannelInfo.read_idle_time, 0, 0))
       // pipeline.addLast("heartBeatHandler", JsonHeartbeatReceiver(clientChannelInfo.read_idle_time, clientChannelInfo.heartbeat_miss_limit))

     //   pipeline.addLast("messageHandler", KonemJsonMessageHandler(clientChannelInfo.channelId, transceiver))

        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
