package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class WireHeartbeatProducer(transceiver: WireTransceiver) :
  HeartbeatProducerHandler<KonemMessage>(transceiver) {

  private val messageTypeHeartBeat = KonemMessage.MessageType.HEARTBEAT

  override fun generateHeartBeat(): KonemMessage {

    return KonemMessage(
      messageType = messageTypeHeartBeat,
      heartBeat = KonemMessage.HeartBeat(Date().toString())
    )
  }
}