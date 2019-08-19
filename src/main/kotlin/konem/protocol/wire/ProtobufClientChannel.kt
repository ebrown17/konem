package konem.protocol.wire

import konem.netty.stream.client.ClientChannel

import io.netty.channel.Channel
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.stream.ExceptionHandler

class ProtobufClientChannel(private val transceiver: ProtobufTransceiver) :
  ClientChannel() {

 override fun initChannel(channel: Channel) {
    val pipeline = channel.pipeline()

    pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
    pipeline.addLast("protobufDecoder", WireDecoder())
    pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast("protobufEncoder", WireEncoder())
    pipeline.addLast("idleStateHandler", IdleStateHandler(READ_IDLE_TIME, 0, 0))
    pipeline.addLast("messagehandler", ProtobufMessageHandler(channelIds.incrementAndGet(), transceiver))
    pipeline.addLast(
      "heartBeatHandler",
      ProtobufHeartbeatReceiver(READ_IDLE_TIME, HEARTBEAT_MISS_LIMIT)
    )
    pipeline.addLast("exceptionHandler", ExceptionHandler())
  }
}