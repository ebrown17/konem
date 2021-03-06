package konem.protocol.socket.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.data.json.Message
import konem.netty.stream.HeartbeatReceiverHandler
import org.slf4j.LoggerFactory

class JsonHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
  HeartbeatReceiverHandler<KonemMessage>(expectedInterval, missLimit) {

  private val logger = LoggerFactory.getLogger(JsonHeartbeatReceiver::class.java)

  override fun channelRead(ctx: ChannelHandlerContext, message: Any) {

    when (message) {
      is KonemMessage -> {
        when (message.konemMessage) {
          is Message.Heartbeat -> {
            logger.trace(
              "received {} from {}",
              Message.Heartbeat,
              ctx.channel().remoteAddress()
            )
            resetMissCounter()
          }
          else -> {
            ctx.fireChannelRead(message)
          }
        }
      }
      else -> {
        ctx.fireChannelRead(message)
      }
    }
  }
}
