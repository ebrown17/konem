package konem.protocol.string



/*
class StringHeartbeatProducer(transceiver: StringServerTransceiver) :
    HeartbeatProducerHandler<String>(transceiver) {
    val heartbeat = "::heartbeat::"
    override fun generateHeartBeat(): String {
        return heartbeat
    }
}

class StringHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
    HeartbeatReceiverHandler<String>(expectedInterval, missLimit) {

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        when (message) {
            is String -> {
                when (message) {
                    "::heartbeat::" ->  resetMissCounter()
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
