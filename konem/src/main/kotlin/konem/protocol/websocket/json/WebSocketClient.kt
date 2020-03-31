package konem.protocol.websocket.json

import io.netty.handler.codec.http.websocketx.WebSocketFrame
import java.net.SocketAddress
import java.net.URI
import konem.data.json.KonemMessage
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientTransmitter
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class WebSocketClient(
  private val serverAddress: SocketAddress,
  config: ClientBootstrapConfig<WebSocketFrame, WebSocketFrame>,
  private val fullWSPath: URI
) : Client<WebSocketFrame, WebSocketFrame>(serverAddress, config),
  ClientTransmitter<KonemMessage>, WebSocketClientChannelReader {

  private val logger = LoggerFactory.getLogger(WebSocketClient::class.java)
  private val transceiver = config.transceiver as WebSocketTransceiver
  val readListeners = mutableListOf<Receiver>()

  override fun sendMessage(message: KonemMessage) {
    if (!isActive()) {
      logger.warn("attempted to send data on null or closed channel")
      return
    }
    logger.trace("remote: {} message: {}", channel?.remoteAddress(), message)
    transceiver.transmit(serverAddress, message)
  }

  override fun handleChannelRead(addr: SocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    clientScope.launch {
      readMessage(addr, channelPort, webSocketPath, message)
    }
  }

  override suspend fun readMessage(addr: SocketAddress, channelPort: Int, webSocketPath: String, message: Any) {
    logger.trace("got message: {}", message)
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
