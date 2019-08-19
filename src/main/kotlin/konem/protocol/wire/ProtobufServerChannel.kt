package konem.protocol.wire

import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.server.ServerChannel

class ProtobufServerChannel(private val transceiver: ProtobufTransceiver) : ServerChannel() {

  override fun initChannel(channel: SocketChannel) {
    val pipeline = channel.pipeline()
    pipeline.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
    pipeline.addLast("protobufDecoder", WireDecoder())
    pipeline.addLast("frameEncoder", ProtobufVarint32LengthFieldPrepender())
    pipeline.addLast("protobufEncoder", WireEncoder())
    pipeline.addLast("messagehandler", ProtobufMessageHandler(channelIds.incrementAndGet(), transceiver))
    pipeline.addLast("idleStateHandler", IdleStateHandler(0, WRITE_IDLE_TIME, 0))
    pipeline.addLast("heartBeatHandler", ProtobufHeartbeatProducer(transceiver))
    pipeline.addLast("exceptionHandler", ExceptionHandler())
  }
}
