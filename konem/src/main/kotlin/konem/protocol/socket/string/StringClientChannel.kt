package konem.protocol.socket.string

import io.netty.channel.Channel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.SslContextManager
import konem.netty.tcp.client.ClientChannel


class StringClientChannel(private val transceiver: StringTransceiver) :
    ClientChannel() {

    override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        pipeline.addLast("clientSslHandler", SslContextManager.getClientContext().newHandler(channel.alloc()))
        pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
        pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
        pipeline.addLast("idleStateHandler", IdleStateHandler(READ_IDLE_TIME, 0, 0))
        pipeline.addLast("messageHandler", StringMessageHandler(channelIds.incrementAndGet(), transceiver))
       // pipeline.addLast("heartBeatHandler", JsonHeartbeatReceiver(READ_IDLE_TIME, HEARTBEAT_MISS_LIMIT))
        pipeline.addLast("exceptionHandler", ExceptionHandler())
    }
}
