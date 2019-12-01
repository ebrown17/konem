package konem.protocol.socket.json

import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.timeout.IdleStateHandler
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.server.ServerChannel

class JsonServerChannel(private val transceiver: JsonTransceiver) : ServerChannel() {

  override fun initChannel(channel: SocketChannel) {
    val pipeline = channel.pipeline()
    pipeline.addLast("jsonDecoder",JsonObjectDecoder())
    pipeline.addLast("konemCodec", JsonKonemCodec())
    pipeline.addLast("frameEncoder", JsonMessageHandler(channelIds.incrementAndGet(), transceiver))
    pipeline.addLast("idleStateHandler", IdleStateHandler(0, WRITE_IDLE_TIME, 0))
    pipeline.addLast("heartBeatHandler", JsonHeartbeatProducer(transceiver))
    pipeline.addLast("exceptionHandler", ExceptionHandler())
  }
}
