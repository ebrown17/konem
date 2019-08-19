package konem.protocol.wire

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemMessage
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class ProtobufMessageHandler(
  handlerId: Long,
  val transceiver: ProtobufTransceiver
) : Handler<KonemMessage>(handlerId, transceiver) {

  private val logger = LoggerFactory.getLogger(ProtobufMessageHandler::class.java)

  override fun channelRead0(ctx: ChannelHandlerContext?, message: KonemMessage?) {
    logger.info("channelRead0 {} sent: {}", remoteAddress, message.toString())

    when (message?.messageType) {
      KonemMessage.MessageType.DATA -> {
        transceiver.handleMessage(remoteAddress,message)

      }
      else ->{
        ctx?.fireChannelRead(message)
      }
    }
  }
}