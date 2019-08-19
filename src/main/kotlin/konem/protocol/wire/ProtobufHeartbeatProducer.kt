package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class ProtobufHeartbeatProducer(transceiver:  ProtobufTransceiver) :
  HeartbeatProducerHandler<KonemMessage>(transceiver) {

  private val heartBuilder = KonemMessage.HeartBeat.Builder()



  private val messageType = KonemMessage.MessageType.HEARTBEAT

  override fun generateHeartBeat():KonemMessage {
    val heartbeat =heartBuilder.time(Date().toString()).build()
    return KonemMessage.Builder().messageType(messageType).heartBeat(heartbeat).build()
  }
}