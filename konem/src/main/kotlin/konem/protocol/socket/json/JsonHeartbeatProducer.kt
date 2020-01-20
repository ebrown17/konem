package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.netty.stream.HeartbeatProducerHandler

class JsonHeartbeatProducer(transceiver: JsonTransceiver) :
  HeartbeatProducerHandler<String>(transceiver) {
  private val serializer = KonemMessageSerializer()
  override fun generateHeartBeat(): String {
    return serializer.toJson(KonemMessage(Message.Heartbeat()))
  }
}
