package konem.protocol.socket.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.SocketAddress

class WireClient(private val serverAddress: SocketAddress, config: ClientBootstrapConfig) :
  Client(serverAddress, config), ClientTransmitter<KonemMessage>, WireClientChannelReader {

  private val logger = LoggerFactory.getLogger(WireClient::class.java)
  private val transceiver = config.transceiver as WireTransceiver
  private val receiveListeners: ArrayList<Receiver> = ArrayList()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("sendMessage attempted to send data on null or closed channel")
      return
    }
    logger.trace("sendMessage remote: {} message: {}", channel?.remoteAddress(), message)
    transceiver.transmit(serverAddress, message)
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    receiveListeners.add(receiver)
  }

  override fun handleChannelRead(addr: SocketAddress, port: Int, message: Any) {
    clientScope.launch {
      readMessage(addr, port, message)
    }
  }

  override suspend fun readMessage(addr: SocketAddress, port: Int, message: Any) {
    logger.trace("readMessage got message: {}", message)
      for (listener in receiveListeners) {
        listener.handle(addr, message)
      }
  }

  override fun toString(): String {
    return "WireClient{Transceiver=$transceiver}"
  }
}
