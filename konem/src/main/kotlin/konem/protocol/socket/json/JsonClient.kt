package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

class JsonClient(private val serverAddress: InetSocketAddress, config: ClientBootstrapConfig) :
  Client(serverAddress, config), ClientTransmitter<KonemMessage>, JsonClientChannelReader {

  private val logger = LoggerFactory.getLogger(JsonClient::class.java)
  private val transceiver = config.transceiver as JsonTransceiver
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

  override fun handleChannelRead(addr: InetSocketAddress, port: Int, message: Any) {
    clientScope.launch {
      readMessage(addr, port, message)
    }
  }

  override suspend fun readMessage(addr: InetSocketAddress, port: Int, message: Any) {
    logger.trace("got message: {}", message)
    for (listener in receiveListeners) {
      listener.handle(addr, message)
    }
  }

  override fun toString(): String {
    return "WireClient{Transceiver=$transceiver}"
  }
}