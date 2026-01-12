package konem.netty

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import konem.logger
import java.net.InetSocketAddress

open class ClientHeartbeatProtocol(
    val enabled: Boolean = true,
    val read_idle_time: Int = 12,
    val miss_limit: Int = 2,
    val dropHeartbeat: Boolean = true,
    val isHeartbeat: (message: Any) -> Boolean
)

class DisabledClientHeartbeatProtocol : ClientHeartbeatProtocol(enabled = false, isHeartbeat = { false })

open class ServerHeartbeatProtocol(
    val enabled: Boolean = true,
    val write_idle_time: Int = 10,
    val generateHeartbeat: () -> Any
)

class DisabledServerHeartbeatProtocol :
    ServerHeartbeatProtocol(enabled = false, generateHeartbeat = { Any() })


class HeartbeatProducer(val generateHeartbeat: () -> Any) :
    ChannelDuplexHandler() {

    private val logger = logger(javaClass)

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.WRITER_IDLE) {
                logger.info("send heartBeat")
                ctx.writeAndFlush(generateHeartbeat()).addListener { future ->
                    if (future.isSuccess) {
                        logger.trace("Heartbeat sent successfully")
                    } else {
                        logger.error("Failed to send ping", future.cause())
                        ctx.close()
                    }
                }
            }
        }
        else{
            super.userEventTriggered(ctx, evt)
        }
    }
}

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

class HeartbeatReceiver(
    private val expectedInterval: Int,
    private val missedLimit: Int,
    private val dropHeartbeat: Boolean,
    private val isHeartbeat: (message: Any) -> Boolean
) : ChannelDuplexHandler() {

    private val logger = logger(javaClass)
    private var missCount = 0

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        if (isHeartbeat(message)) {
            logger.trace("received {} heartbeat", message)
            missCount = 0
            if (dropHeartbeat){
                return
            }
        }
        ctx.fireChannelRead(message)
    }

    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            logger.info("{}, idle count is {}", evt.state(), missCount)

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
        else{
            super.userEventTriggered(ctx, evt)
        }
    }
}
