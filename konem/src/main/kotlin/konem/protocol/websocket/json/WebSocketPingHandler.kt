package konem.protocol.websocket.json

import konem.netty.stream.HeartbeatProducerHandler
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

class WebSocketPingHandler(transceiver: WebSocketTransceiver) :
  HeartbeatProducerHandler<WebSocketFrame>(transceiver) {

  override fun generateHeartBeat(): WebSocketFrame {
    return PingWebSocketFrame()
  }
}
