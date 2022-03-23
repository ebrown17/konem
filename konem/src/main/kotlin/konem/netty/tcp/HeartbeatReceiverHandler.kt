package konem.netty.tcp

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import konem.logger

/**
 * HeartbeatReceiverHandler expects to be receiving a heartbeat message.
 *
 * If the heartbeat miss limit is reached the channel is closed and the client's reconnect logic is
 * started.
 *
 * By default every channel read will reset the miss count. To only reset on a heartbeat, you must override the
 * channelRead method and add appropriate logic. The method
 *
 * [resetMissCounter ][HeartbeatReceiverHandler.resetMissCounter] can be called to reset the miss count.
 *
 * @param expectedInterval The expected heartbeat interval in seconds. This will be used to determine if server
 * is no longer alive.
 * @param missedLimit The max amount of heartbeats allowed until handler closes channel.
 */

abstract class HeartbeatReceiverHandler<I>(
    private val expectedInterval: Int,
    private val missedLimit: Int
) : ChannelDuplexHandler() {

    private val logger = logger(javaClass)
    private var missCount = 0

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            logger.info("{} miss count {}", evt.state(), missCount)

            if (evt.state() == IdleState.READER_IDLE) {
                if (missCount >= missedLimit) {
                    logger.info(
                        "no heartbeat read for {} seconds. Closing Connection.",
                        missedLimit * expectedInterval
                    )
                    ctx.close()
                }
                missCount++
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
