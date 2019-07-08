package konem.protocol.websocket

import konem.netty.stream.server.ServerChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.timeout.IdleStateHandler

class WebSocketServerChannel(transceiver: WebSocketTransceiver, vararg webSocketPaths: String) :
  ServerChannel() {

  private val transceiver: WebSocketTransceiver
  private val webSocketPaths: Array<String>

  init {
    if (transceiver is WebSocketTransceiver) {
      this.transceiver = transceiver
      this.webSocketPaths = arrayOf(*webSocketPaths)
    } else {
      throw IllegalArgumentException("Transceiver must be of type " + WebSocketTransceiver::class.java)
    }
  }

  @Throws(Exception::class)
  override fun initChannel(channel: SocketChannel) {
    val pipeline = channel.pipeline()
    pipeline.addLast("httpServerCodec", HttpServerCodec())
    pipeline.addLast("httpAggregator", HttpObjectAggregator(65536))
    pipeline.addLast("compressionHandler", WebSocketServerCompressionHandler())
    pipeline.addLast(
      WebSocketPathHandler::class.java.name,
      WebSocketPathHandler(transceiver, channelIds, *webSocketPaths)
    )
    pipeline.addLast("idleStateHandler", IdleStateHandler(0, WRITE_IDLE_TIME, 0))
    pipeline.addLast("pingHandler", WebSocketPingHandler(transceiver))
    pipeline.addLast("exceptionHandler", WebSocketExceptionHandler())
  }
}
