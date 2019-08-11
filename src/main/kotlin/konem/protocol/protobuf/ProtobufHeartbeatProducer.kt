package konem.protocol.protobuf

import konem.data.protobuf.KonemPMessage
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class ProtobufHeartbeatProducer(transceiver:  ProtobufTransceiver) :
  HeartbeatProducerHandler<KonemPMessage>(transceiver) {

  val builder = KonemPMessage.Builder().heartBeat.newBuilder()

  override fun generateHeartBeat(): KonemPMessage {
    val heartbeat =builder.time(Date().toString()).build()
    return KonemPMessage.Builder().heartBeat(heartbeat).build()
  }
}