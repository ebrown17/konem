package konem.netty.stream

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

abstract class HeartbeatProducerHandler<I>(private val transceiver: Transceiver<I>) :
  ChannelDuplexHandler() {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Throws(Exception::class)
  override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    if (evt is IdleStateEvent) {
      if (evt.state() == IdleState.WRITER_IDLE) {
        logger.trace("userEventTriggered send heartBeat")
        transceiver.transmit(
          ctx.channel().remoteAddress() as InetSocketAddress,
          generateHeartBeat()
        )
      }
    }
  }

  abstract fun generateHeartBeat(): I

}