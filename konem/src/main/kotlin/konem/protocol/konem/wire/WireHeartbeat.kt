package konem.protocol.konem.wire


/*class WireHeartbeatProducer(transceiver: WireServerTransceiver) :
    HeartbeatProducerHandler<KonemMessage>(transceiver) {
    override fun generateHeartBeat(): KonemMessage {
        return KonemMessage(
            messageType = MessageType.HEARTBEAT,
            heartBeat = HeartBeat(Date().toString())
        )
    }
}

class WireHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
    HeartbeatReceiverHandler<String>(expectedInterval, missLimit) {

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {
        when (message) {
            is KonemMessage -> {
                when (message.messageType) {
                    MessageType.HEARTBEAT -> resetMissCounter()
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
}*/
