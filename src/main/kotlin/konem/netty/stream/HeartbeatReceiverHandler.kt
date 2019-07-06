package konem.netty.stream

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import org.slf4j.LoggerFactory

/**
 * HeartbeatReceiverHandler expects to be receiving a heartbeat message.
 *
 * If the heartbeat miss limit is reached the channel is closed and the client's reconnect logic is
 * started.
 *
 * By default every channel read will reset the miss count. To only reset on a heartbeat, you must override the channelRead
 * method and add appropriate logic. The method
 * [resetMissCounter ][HeartbeatReceiverHandler.resetMissCounter] can be called to reset the miss count.
 *
 * @param expectedInterval The expected heartbeat interval in seconds. This will be used to determine if server
 * is no longer alive.
 * @param missedLimit      The max amount of heartbeats allowed until handler closes channel.
 */

abstract class HeartbeatReceiverHandler<I>(
  private val expectedInterval: Int,
  private val missedLimit: Int
) : ChannelDuplexHandler() {

  private val logger = LoggerFactory.getLogger(javaClass)
  private var missCount = 0

  @Throws(Exception::class)
  override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
    logger.trace("userEventTriggered")
    if (evt is IdleStateEvent) {
      logger.trace("userEventTriggered {} miss count {}", evt.state(), missCount)

      if (evt.state() == IdleState.READER_IDLE) {
        if (missCount >= missedLimit) {
          logger.info(
            "userEventTriggered no heartbeat read for {} seconds. Closing Connection.",
            missedLimit * expectedInterval
          )
          ctx.close()
        } else {
          missCount++
        }
      }
    }
  }

  /**
   * Sets the heartbeat miss counter to zero.
   */
  protected fun resetMissCounter() {
    missCount = 0
  }
}
