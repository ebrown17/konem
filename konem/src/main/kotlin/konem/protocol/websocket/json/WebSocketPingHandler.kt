package konem.protocol.websocket.json

import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import konem.netty.stream.HeartbeatProducerHandler

class WebSocketPingHandler(transceiver: WebSocketTransceiver) :
  HeartbeatProducerHandler<WebSocketFrame, WebSocketFrame>(transceiver) {

  override fun generateHeartBeat(): WebSocketFrame {
    return PingWebSocketFrame()
  }
}
