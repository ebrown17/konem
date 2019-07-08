package konem.netty.stream.client

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class ClientClosedConnectionListener internal constructor(private val client: Client) :
  ChannelFutureListener {

  @Throws(InterruptedException::class)
  override fun operationComplete(future: ChannelFuture) {
    if (client.isDisconnectInitiated) {
      future.channel().close().awaitUninterruptibly(1, TimeUnit.SECONDS)
      logger.info("connect.closeFuture > Client fully disconnected")
    } else {
      future.channel().eventLoop().schedule({
        try {
          client.connect()
        } catch (e: InterruptedException) {
          logger.error("operationComplete {}", e.message, e)
          throw RuntimeException("Interrupted trying to connect")
        }
      }, client.calculateRetryTime(), TimeUnit.SECONDS)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(ClientClosedConnectionListener::class.java)
  }
}
