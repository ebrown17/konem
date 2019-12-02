package konem.protocol.socket.json

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class JsonTransceiver(channelPort: Int) : Transceiver<String>(channelPort) {
  private val logger = LoggerFactory.getLogger(JsonTransceiver::class.java)
  private val serializer = KonemMessageSerializer()

  fun handleMessage(addr: InetSocketAddress, message: String) {
    logger.trace("from {} with {}", addr, message)
    val reader = channelReaders[addr] as JsonChannelReader
    logger.trace("channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader.handleChannelRead(addr, channelPort, message)
  }

  /**
   * Sends a message to specified address if connected to this transceiver
   * @param addr
   * @param message
   */
  fun transmit(addr: InetSocketAddress, message: KonemMessage) {
    synchronized(activeLock) {
      logger.debug("to addr: {} with {}", addr, message)
      val handler = activeHandlers[addr]
      val kMessage = serializer.toJson(message)
      handler?.sendMessage(kMessage)
    }
  }

  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * @param message
   */
  fun broadcastMessage(message: KonemMessage) {
    logger.debug("message: {}", message)
    val jsonMessage = serializer.toJson(message)
    synchronized(activeLock) {
      for (handler in activeHandlers.values) {
        handler.sendMessage(jsonMessage)
      }
    }
  }
}
