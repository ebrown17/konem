package konem.protocol.konem.json


/*
class JsonHeartbeatProducer(transceiver: JsonServerTransceiver) :
    HeartbeatProducerHandler<KonemMessage>(transceiver) {
    override fun generateHeartBeat(): KonemMessage {
        return KonemMessage(Heartbeat())
    }
}

class JsonHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
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
*/
