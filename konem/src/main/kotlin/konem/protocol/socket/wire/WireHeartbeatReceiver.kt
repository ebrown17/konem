package konem.protocol.socket.wire

import io.netty.channel.ChannelHandlerContext
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.stream.HeartbeatReceiverHandler
import org.slf4j.LoggerFactory

class WireHeartbeatReceiver(expectedInterval: Int, missLimit: Int) :
    HeartbeatReceiverHandler<KonemMessage>(expectedInterval, missLimit) {

    private val logger = LoggerFactory.getLogger(WireHeartbeatReceiver::class.java)

    override fun channelRead(ctx: ChannelHandlerContext, message: Any) {

        when (message) {
            is KonemMessage -> {
                when (message.messageType) {
                    MessageType.HEARTBEAT -> {
                        logger.trace(
                            "received {} from {}",
                            message.messageType,
                            ctx.channel().remoteAddress()
                        )
                        resetMissCounter()
                    }
                    else -> {
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
