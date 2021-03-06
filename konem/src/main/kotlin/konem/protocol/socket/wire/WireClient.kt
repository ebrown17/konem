package konem.protocol.socket.wire

import java.net.SocketAddress
import konem.data.protobuf.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class WireClient(private val serverAddress: SocketAddress, config: ClientBootstrapConfig<KonemMessage, KonemMessage>) :
  Client<KonemMessage, KonemMessage>(serverAddress, config), ClientTransmitter<KonemMessage>, WireClientChannelReader {

  private val logger = LoggerFactory.getLogger(WireClient::class.java)
  private val transceiver = config.transceiver as WireTransceiver
  private val receiveListeners: ArrayList<Receiver> = ArrayList()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("attempted to send data on null or closed channel")
      return
    }
    logger.trace("remote: {} message: {}", channel?.remoteAddress(), message)
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
    logger.trace("got message: {}", message)
      for (listener in receiveListeners) {
        listener.handle(addr, message)
      }
  }

  override fun toString(): String {
    return "WireClient{Transceiver=$transceiver}"
  }
}
