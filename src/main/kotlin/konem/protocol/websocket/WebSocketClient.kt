package konem.protocol.websocket

import konem.data.json.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URI

class WebSocketClient(
  private val serverAddress: InetSocketAddress,
  config: ClientBootstrapConfig,
  private val fullWSPath: URI
) : Client(serverAddress, config),
  ClientTransmitter<KonemMessage>, WebSocketClientChannelReader {

  private val logger = LoggerFactory.getLogger(WebSocketClient::class.java)
  private val transceiver = config.transceiver as WebSocketTransceiver
  val readListeners = mutableListOf<Receiver>()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("sendMessage attempted to send data on null or closed channel")
      return
    }
    logger.trace("sendMessage remote: {} message: {}", channel?.remoteAddress(), message)
    transceiver.transmit(serverAddress, message)
  }

  override fun handleChannelRead(addr: InetSocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    clientScope.launch {
      readMessage(addr, channelPort, webSocketPath, message)
    }
  }

  override suspend fun readMessage(addr: InetSocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    logger.trace("readMessage got message: {}", message)
    for (listener in readListeners) {
      listener.handle(addr, message)
    }
  }

  override fun registerChannelReadListener(receiver: Receiver) {
    readListeners.add(receiver)
  }

  override fun registerChannelReadListener(receiver: Receiver, vararg args: String) {
    registerChannelReadListener(receiver)
  }

  override fun toString(): String {
    return "WebSocketClient{WsUrl=$fullWSPath, $transceiver}"
  }
}
