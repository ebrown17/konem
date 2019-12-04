package konem.protocol.socket.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.KonemMessage
import konem.data.json.Message
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class JsonMessageHandler(
  handlerId: Long,
  val transceiver: JsonTransceiver
) : Handler<String>(handlerId, transceiver) {

  private val logger = LoggerFactory.getLogger(JsonMessageHandler::class.java)

  override fun channelRead0(ctx: ChannelHandlerContext, message: String) {
    logger.info("{} sent: {}", remoteAddress, message)
    transceiver.handleMessage(remoteAddress, message)

  }
}
