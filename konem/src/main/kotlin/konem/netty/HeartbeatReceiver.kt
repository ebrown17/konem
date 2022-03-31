package konem.netty

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import konem.logger

/**
 * HeartbeatReceiver receives a heartbeat message.
 *
 * If the heartbeat miss limit is reached the channel is closed and the client's reconnect logic is
 * started.
 *
 * Resets only happen on a heartbeat
 *
 * @param expectedInterval The expected heartbeat interval in seconds. This will be used to determine if server
 * is no longer alive.
 * @param missedLimit The max amount of heartbeats allowed until handler closes channel.
 * @param isHeartbeat Function to determine if message of type I is a heartbeat; Should return true if heartbeat else false
 */

class HeartbeatReceiver<I>(
    private val expectedInterval: Int,
    private val missedLimit: Int,
    private val isHeartbeat: (message:I) -> Boolean
) : SimpleChannelInboundHandler<I>() {

    private val logger = logger(javaClass)
    private var missCount = 0

    override fun channelRead0(ctx: ChannelHandlerContext, message: I) {
        if(isHeartbeat(message)){
            missCount = 0
        }
        else{
            ctx.fireChannelRead(message)
        }
    }

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            logger.trace("{} miss count {}", evt.state(), missCount)

            if (evt.state() == IdleState.READER_IDLE) {
                if (missCount >= missedLimit) {
                    logger.warn(
                        "no heartbeat read for {} seconds. Closing Connection.",
                        missedLimit * expectedInterval
                    )
                    ctx.close()
                }
                missCount++
            }
        }
    }
}
