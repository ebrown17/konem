package konem.protocol.websocket

import io.netty.channel.Channel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler
import konem.netty.stream.client.ClientChannel
import java.net.URI

class WebSocketClientChannel(
  private val transceiver: WebSocketTransceiver,
  private val webSocketPath: URI
) : ClientChannel() {

  private val clientHandShaker: WebSocketClientHandshaker
    get() = WebSocketClientHandshakerFactory.newHandshaker(
      webSocketPath, WebSocketVersion.V13, null, true, DefaultHttpHeaders()
    )

  @Throws(Exception::class)
  override fun initChannel(channel: Channel) {
    val pipeline = channel.pipeline()
    pipeline.addLast("clientCodec", HttpClientCodec())
    pipeline.addLast("aggregator", HttpObjectAggregator(Short.MAX_VALUE.toInt()))
    pipeline.addLast("compressionHandler", WebSocketClientCompressionHandler.INSTANCE)
    pipeline.addLast("clientHandler", WebSocketClientProtocolHandler(clientHandShaker, true, false))
    pipeline.addLast(
      "frameHandler",
      WebSocketFrameHandler(channelIds.incrementAndGet(), transceiver, webSocketPath.path)
    )
    pipeline.addLast("exceptionHandler", WebSocketExceptionHandler())

  }

}