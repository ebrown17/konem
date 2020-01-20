package konem.protocol.socket.json

import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.json.JsonObjectDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import konem.netty.stream.ExceptionHandler
import konem.netty.stream.SslContextManager
import konem.netty.stream.server.ServerChannel

class JsonServerChannel(private val transceiver: JsonTransceiver) : ServerChannel() {

  override fun initChannel(channel: SocketChannel) {
    val pipeline = channel.pipeline()
    pipeline.addLast("serverSslHandler", SslContextManager.getServerContext().newHandler(channel.alloc()));
    pipeline.addLast("jsonDecoder", JsonObjectDecoder())
    pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8))
    pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))
    pipeline.addLast("jsonHandler", JsonMessageHandler(channelIds.incrementAndGet(), transceiver))
    pipeline.addLast("idleStateHandler", IdleStateHandler(0, WRITE_IDLE_TIME, 0))
    pipeline.addLast("heartBeatHandler", JsonHeartbeatProducer(transceiver))
    pipeline.addLast("exceptionHandler", ExceptionHandler())
  }
}
