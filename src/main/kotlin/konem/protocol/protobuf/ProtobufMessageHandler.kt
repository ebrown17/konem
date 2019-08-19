package konem.protocol.protobuf

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemProtoMessage
import konem.netty.stream.Handler
import org.slf4j.LoggerFactory

class ProtobufMessageHandler(
  handlerId: Long,
  val transceiver: ProtobufTransceiver
) : Handler<KonemProtoMessage.KonemMessage>(handlerId, transceiver) {

  private val logger = LoggerFactory.getLogger(ProtobufMessageHandler::class.java)

  override fun channelRead0(ctx: ChannelHandlerContext?, message: KonemProtoMessage.KonemMessage?) {
    logger.trace("channelRead0 {} sent: {}", remoteAddress, message.toString())

    when (message?.messageType) {
      KonemProtoMessage.KonemMessage.MessageType.DATA -> {
        transceiver.handleMessage(remoteAddress,message)
      }
      else ->{
        ctx?.fireChannelRead(message)
      }
    }
  }
}