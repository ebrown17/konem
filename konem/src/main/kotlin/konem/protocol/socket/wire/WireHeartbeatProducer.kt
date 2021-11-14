package konem.protocol.socket.wire

import konem.data.protobuf.HeartBeat
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.stream.HeartbeatProducerHandler
import java.util.*

class WireHeartbeatProducer(transceiver: WireTransceiver) :
    HeartbeatProducerHandler<KonemMessage, KonemMessage>(transceiver) {

    private val messageTypeHeartBeat = MessageType.HEARTBEAT

    override fun generateHeartBeat(): KonemMessage {

        return KonemMessage(
            messageType = messageTypeHeartBeat,
            heartBeat = HeartBeat(Date().toString())
        )
    }
}
