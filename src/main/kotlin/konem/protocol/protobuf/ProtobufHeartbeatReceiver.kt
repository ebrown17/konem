package konem.protocol.protobuf

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemPMessage
import konem.netty.stream.HeartbeatReceiverHandler
import org.slf4j.LoggerFactory

class ProtobufHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
  HeartbeatReceiverHandler<KonemPMessage>(expectedInterval, missLimit) {

  private val logger = LoggerFactory.getLogger(ProtobufHeartbeatReceiver::class.java)

  override fun channelRead(ctx: ChannelHandlerContext?, message: Any) {

    when (message) {
      is KonemPMessage -> {
        when(message.messageType){
          KonemPMessage.MessageType.HEARTBEAT -> {
            logger.trace(
              "channelRead received {} from {}",
              message.messageType,
              ctx?.channel()?.remoteAddress()
            )
            resetMissCounter()
          }
          else  -> {
            ctx?.fireChannelRead(message)
          }
        }
      }
      else -> {
        ctx?.fireChannelRead(message)
      }
    }
  }
}
