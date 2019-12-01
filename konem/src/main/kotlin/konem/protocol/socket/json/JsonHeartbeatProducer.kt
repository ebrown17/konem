package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class JsonHeartbeatProducer(transceiver: JsonTransceiver) :
  HeartbeatProducerHandler<KonemMessage>(transceiver) {

  override fun generateHeartBeat(): KonemMessage {
    return KonemMessage(Message.Heartbeat())
  }
}
