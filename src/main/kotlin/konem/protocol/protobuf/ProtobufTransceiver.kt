package konem.protocol.protobuf

import konem.data.protobuf.KonemPMessage
import konem.netty.stream.Transceiver
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class ProtobufTransceiver(channelPort: Int) : Transceiver<KonemPMessage>(channelPort) {
  private val noop = "NOOP"
  private val logger = LoggerFactory.getLogger(ProtobufTransceiver::class.java)

  fun handleMessage(addr: InetSocketAddress, message: KonemPMessage) {
    logger.trace("handleMessage from {} with {}", addr, message)
    val reader = channelReaders[addr]
    logger.trace("handleMessage channelReaders: {} reader got: {}", channelReaders.size, reader)
    reader?.handleChannelRead(addr, noop, message)
  }


  /**
   * Broadcast a message on all channels connected to this port's transceiver.
   * Can also broadcast to a specific websocket path or paths
   * @param message
   * @param websocketPaths
   */
  fun broadcastMessage(message: KonemPMessage) {
    logger.debug("broadcastMessage message: {}", message)
    synchronized(activeLock) {
      for (handler in activeHandlers.values) {
        handler.sendMessage(message)
      }
    }
  }
}