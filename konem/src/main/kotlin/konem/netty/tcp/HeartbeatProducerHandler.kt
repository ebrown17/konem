package konem.netty.tcp

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import konem.logger
import java.net.InetSocketAddress

abstract class HeartbeatProducerHandler<I>(private val transceiver: ServerTransceiver<I>) :
    ChannelDuplexHandler() {

    private val logger = logger(javaClass)

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.WRITER_IDLE) {
                logger.trace("send heartBeat")
                transceiver.transmit(
                    ctx.channel().remoteAddress() as InetSocketAddress,
                    generateHeartBeat()
                )
            }
        }
    }

    abstract fun generateHeartBeat(): I
}