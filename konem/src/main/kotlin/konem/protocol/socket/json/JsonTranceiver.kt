package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class JsonTransceiver(channelPort: Int) : Transceiver<KonemMessage>(channelPort) {
  private val logger = LoggerFactory.getLogger(JsonTransceiver::class.java)

  fun handleMessage(addr: InetSocketAddress, message: KonemMessage) {
    logger.trace("from {} with {}", addr, message)
    val reader = channelReaders[addr] as JsonChannelReader
    logger.trace("channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader.handleChannelRead(addr, channelPort, message)
  }

  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * @param message
   */
  fun broadcastMessage(message: KonemMessage) {
    logger.debug("message: {}", message)
    synchronized(activeLock) {
      for (handler in activeHandlers.values) {
        handler.sendMessage(message)
      }
    }
  }
}
