package konem.protocol.konem.json

import io.netty.channel.ChannelHandlerContext
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.tcp.HeartbeatProducerHandler
import konem.netty.tcp.HeartbeatReceiverHandler


class KonemHeartbeatProducer(transceiver: KonemServerTransceiver) :
    HeartbeatProducerHandler<KonemMessage>(transceiver) {
    override fun generateHeartBeat(): KonemMessage {
        return KonemMessage(Heartbeat())
    }
}

class KonemHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
    HeartbeatReceiverHandler<String>(expectedInterval, missLimit) {

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        when (message) {
            is KonemMessage -> {
                when (message.message) {
                    is Heartbeat ->  resetMissCounter()
                    else -> {
                        resetMissCounter()
                        ctx.fireChannelRead(message)
                    }
                }
            }
            else -> {
                ctx.fireChannelRead(message)
            }
        }
    }
}
