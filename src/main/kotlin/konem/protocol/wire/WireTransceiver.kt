package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class WireTransceiver(channelPort: Int) : Transceiver<KonemMessage>(channelPort) {
  private val logger = LoggerFactory.getLogger(WireTransceiver::class.java)

  fun handleMessage(addr: InetSocketAddress, message: KonemMessage) {
    logger.trace("handleMessage from {} with {}", addr, message)
    val reader = channelReaders[addr] as WireChannelReader
    logger.trace("handleMessage channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader.handleChannelRead(addr, channelPort, message)
  }

  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * @param message
   */
  fun broadcastMessage(message: KonemMessage) {
    logger.debug("broadcastMessage message: {}", message)
    synchronized(activeLock) {
      for (handler in activeHandlers.values) {
        handler.sendMessage(message)
      }
    }
  }
}
