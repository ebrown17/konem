package konem.protocol.websocket

import konem.netty.stream.Transceiver
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class WebSocketTransceiver(channelPort: Int) : Transceiver<WebSocketFrame>(channelPort) {
  private val logger = LoggerFactory.getLogger(WebSocketTransceiver::class.java)

  fun handleMessage(addr: InetSocketAddress, webSocketPath: String, message: String) {
    logger.trace("handleMessage from {} with {}", addr, message)
    val reader = channelReaders[addr]
    logger.trace("handleMessage channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader?.handleChannelRead(addr, webSocketPath, message)
  }

  /**
   * Sends a message to specified address if connected to this transceiver
   * @param addr
   * @param message
   */
  fun transmit(addr: InetSocketAddress, message: String) {
    synchronized(activeLock) {
      logger.debug("sendMessage to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      handler?.sendMessage(TextWebSocketFrame(message))
    }
  }

  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * Can also broadcast to a specific websocket path or paths
   * @param message
   * @param websocketPaths
   */
  fun broadcastMessage(message: String, vararg websocketPaths: String) {
    logger.debug("broadcastMessage to paths:{} message: {}", websocketPaths, message)
    val frame = TextWebSocketFrame(message)
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
