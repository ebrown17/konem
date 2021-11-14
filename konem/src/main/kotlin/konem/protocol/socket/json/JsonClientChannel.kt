package konem.protocol.socket.json

import io.netty.channel.Channel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.SslContextManager
import konem.netty.stream.client.ClientChannel

class JsonClientChannel(private val transceiver: JsonTransceiver) :
    ClientChannel() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        pipeline.addLast("clientSslHandler", SslContextManager.getClientContext().newHandler(channel.alloc()))
        pipeline.addLast("jsonDecoder", JsonObjectDecoder())
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("idleStateHandler", IdleStateHandler(READ_IDLE_TIME, 0, 0))
        pipeline.addLast("messageHandler", JsonMessageHandler(channelIds.incrementAndGet(), transceiver))
        pipeline.addLast("heartBeatHandler", JsonHeartbeatReceiver(READ_IDLE_TIME, HEARTBEAT_MISS_LIMIT))
        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
