package konem.protocol.websocket.json

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import java.net.SocketAddress
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory

class WebSocketTransceiver(channelPort: Int) : Transceiver<WebSocketFrame, WebSocketFrame>(channelPort) {
  private val logger = LoggerFactory.getLogger(WebSocketTransceiver::class.java)
  private val serializer = KonemMessageSerializer()

  fun handleMessage(addr: SocketAddress, webSocketPath: String, message: String) {
    logger.trace("from {} with {}", addr, message)
    val reader = channelReaders[addr] as WebSocketChannelReader
    logger.trace("channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader.handleChannelRead(addr, channelPort, webSocketPath, message)
  }

  /**
   * Sends a message to specified address if connected to this transceiver
   * @param addr
   * @param message
   */
  fun transmit(addr: SocketAddress, message: KonemMessage) {
    synchronized(activeLock) {
      logger.debug("to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      val kMessage = serializer.toJson(message)
      val frame = TextWebSocketFrame(kMessage)
      handler?.sendMessage(frame)
    }
  }

  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * Can also broadcast to a specific websocket path or paths
   * @param message
   * @param websocketPaths
   */
  fun broadcastMessage(message: KonemMessage, vararg websocketPaths: String) {
    logger.debug("paths:{} message: {}", websocketPaths, message)
    val kMessage = serializer.toJson(message)
    val frame = TextWebSocketFrame(kMessage)
    try {
      synchronized(activeLock) {
        if (websocketPaths.isEmpty()) {
          for (handler in activeHandlers.values) {
            handler.sendMessage(frame.retainedDuplicate())
          }
        } else {
          for (path in websocketPaths) {
            for (handler in activeHandlers.values) {
              handler as WebSocketFrameHandler
              if (path == handler.webSocketPath) {
                handler.sendMessage(frame.retainedDuplicate())
              }
            }
          }
        }
      }
    } finally {
      frame.release()
    }
  }
}
