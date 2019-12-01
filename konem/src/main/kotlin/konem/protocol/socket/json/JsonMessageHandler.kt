package konem.protocol.socket.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.data.json.Message
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class JsonMessageHandler(
  handlerId: Long,
  val transceiver: JsonTransceiver
) : Handler<KonemMessage>(handlerId, transceiver) {

  private val logger = LoggerFactory.getLogger(JsonMessageHandler::class.java)

  override fun channelRead0(ctx: ChannelHandlerContext, message: KonemMessage) {
    logger.info("{} sent: {}", remoteAddress, message.toString())

    when (message.konemMessage) {
      is Message.Data -> {
        transceiver.handleMessage(remoteAddress, message)
      }
      else -> {
        ctx.fireChannelRead(message)
      }
    }
  }
}
