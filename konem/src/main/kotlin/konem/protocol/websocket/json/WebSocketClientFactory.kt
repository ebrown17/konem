package konem.protocol.websocket.json

import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class WebSocketClientFactory : ClientFactory() {
  private val logger = LoggerFactory.getLogger(WebSocketClientFactory::class.java)
  private val clientArrayList = ArrayList<WebSocketClient>()

  override fun createClient(host: String, port: Int, vararg webSocketPath: String): WebSocketClient {
    val address = InetSocketAddress(host, port)
    val transceiver = WebSocketTransceiver(port)
    return createClient(address, createClientConfig(transceiver), *webSocketPath)
  }

  override fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig,
    vararg webSocketPath: String
  ): WebSocketClient {
      val fullWebSocketUrl = buildFullWebSocketPath(address, webSocketPath[0])
      val transceiver = config.transceiver as WebSocketTransceiver
      val bootstrap = config.bootstrap
      val clientChannel =
        WebSocketClientChannel(transceiver, fullWebSocketUrl)
      bootstrap.handler(clientChannel)
      val client = WebSocketClient(address, config, fullWebSocketUrl)
      clientArrayList.add(client)
      return client
  }

  @Throws(URISyntaxException::class)
  private fun buildFullWebSocketPath(address: InetSocketAddress, webSocketPath: String): URI {
    return URI("ws://" + address.hostString + ":" + address.port + webSocketPath)
  }

  override fun shutdown() {
    for (client in clientArrayList) {
      client.shutdown()
    }
    clientArrayList.clear()
    workerGroup.shutdownGracefully()
  }
}
