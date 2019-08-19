package konem.protocol.protobuf

import com.google.protobuf.Timestamp
import konem.data.protobuf.KonemProtoMessage
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class ProtobufHeartbeatProducer(transceiver:  ProtobufTransceiver) :
  HeartbeatProducerHandler<KonemProtoMessage.KonemMessage>(transceiver) {

  private val heartBuilder = KonemProtoMessage.KonemMessage.newBuilder().heartBeatBuilder
  private val timestampBuilder = Timestamp.newBuilder()
  private val konemMessageBuilder: KonemProtoMessage.KonemMessage.Builder =  KonemProtoMessage.KonemMessage.newBuilder()

  private val messageType = KonemProtoMessage.KonemMessage.MessageType.HEARTBEAT

  override fun generateHeartBeat(): KonemProtoMessage.KonemMessage {
    val timestamp = timestampBuilder.setSeconds(Date().time).build()
    val heartbeat =heartBuilder.setDate(timestamp).build()
    return konemMessageBuilder.setMessageType(messageType).setHeartBeat(heartbeat).build()
  }
}