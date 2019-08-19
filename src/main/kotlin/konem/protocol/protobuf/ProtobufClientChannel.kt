package konem.protocol.protobuf

import konem.netty.stream.client.ClientChannel

import io.netty.channel.Channel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.data.protobuf.KonemProtoMessage
import konem.netty.stream.ExceptionHandler

class ProtobufClientChannel(private val transceiver: ProtobufTransceiver) :
  ClientChannel() {

 override fun initChannel(channel: Channel) {
    val pipeline = channel.pipeline()

    pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
    pipeline.addLast("protobufDecoder", ProtobufDecoder(KonemProtoMessage.KonemMessage.getDefaultInstance()))
    pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast("protobufEncoder", ProtobufEncoder())
    pipeline.addLast("idleStateHandler", IdleStateHandler(READ_IDLE_TIME, 0, 0))
    pipeline.addLast("messagehandler", ProtobufMessageHandler(channelIds.incrementAndGet(), transceiver))
    pipeline.addLast(
      "heartBeatHandler",
      ProtobufHeartbeatReceiver(READ_IDLE_TIME, HEARTBEAT_MISS_LIMIT)
    )
    pipeline.addLast("exceptionHandler", ExceptionHandler())
  }
}
