package konem.protocol.socket.json

import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.HeartbeatProducerHandler

class JsonHeartbeatProducer(transceiver: JsonTransceiver) :
  HeartbeatProducerHandler<KonemMessage, String>(transceiver) {
  private val serializer = KonemMessageSerializer()
  override fun generateHeartBeat(): String {
    return serializer.toJson(KonemMessage(Heartbeat()))
  }
}
