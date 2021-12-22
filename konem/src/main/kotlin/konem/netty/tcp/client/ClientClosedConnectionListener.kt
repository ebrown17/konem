package konem.netty.tcp.client

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import konem.logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ClientClosedConnectionListener<I> internal constructor(
    private val client: ClientInternal<I>,
    private val closeAction: () -> Unit
) : ChannelFutureListener {
    private val logger = logger(javaClass)
    @Throws(InterruptedException::class)
    override fun operationComplete(future: ChannelFuture) {
        if (client.isDisconnectInitiated) {
            future.channel().close().awaitUninterruptibly(1, TimeUnit.SECONDS)
            logger.info("connect.closeFuture > Client fully disconnected")
        } else {
            closeAction()
        }
    }
}
