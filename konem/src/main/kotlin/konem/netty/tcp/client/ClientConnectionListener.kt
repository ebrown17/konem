package konem.netty.tcp.client

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import konem.logger
import java.util.concurrent.TimeUnit

data class RetryInfo(val retry_period : Long, val max_retry_period: Long, var retries_until_period_increase : Int)

class ClientConnectionListener<I> internal constructor(
    private val client: ClientInternal<I>,
    private val retryInfo: RetryInfo,
    private val connectAction: ( future: ChannelFuture) -> Unit
) : ChannelFutureListener {

    private val logger = logger(javaClass)
    internal var isAttemptingConnection = true
        private set
    private var connectionAttempts = 0
    private var lastRetryTime = 0L
    private var connectionAttemptStart = 0L

    @Throws(Exception::class)
    override fun operationComplete(future: ChannelFuture) {
        if (future.isSuccess) {
            isAttemptingConnection = false
            connectAction(future)
        } else {
            future.channel().close()
            future.channel().eventLoop().schedule(
                {
                    try {
                        isAttemptingConnection = true
                        client.connect()
                    } catch (e: InterruptedException) {
                        // TODO test to see what happens if this is reached
                        throw InterruptedException("ClientConnectionListener interrupted while trying to connect")
                    }
                },
                calculateRetryTime(), TimeUnit.SECONDS
            )
        }
    }

    private fun calculateRetryTime(): Long {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRetry = currentTime - lastRetryTime

        if(connectionAttempts > 0){
            connectionAttemptStart += (timeSinceLastRetry / 1_000L )
        }

        connectionAttempts++
        lastRetryTime = currentTime

        if (connectionAttempts >= retryInfo.retries_until_period_increase) {
            logger.debug(
                "current connection attempt {} >= max {}; setting {} as retry interval: total time retrying {} seconds",
                connectionAttempts,
                retryInfo.retries_until_period_increase,
                retryInfo.max_retry_period,
                connectionAttemptStart
            )
            return retryInfo.max_retry_period
        } else {
            logger.debug(
                "current connection attempt {} < max {}; setting {} seconds as retry interval: total time retrying {} seconds",
                connectionAttempts,
                retryInfo.retries_until_period_increase,
                retryInfo.max_retry_period,
                connectionAttemptStart
            )
            return retryInfo.retry_period
        }
    }

}
