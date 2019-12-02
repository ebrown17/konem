package konem.protocol.socket.json


import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList

class JsonClientFactory : ClientFactory() {
  private val logger = LoggerFactory.getLogger(JsonClientFactory::class.java)
  private val clientArrayList = ArrayList<JsonClient>()

  override fun createClient(
    host: String,
    port: Int,
    vararg args: String
  ): JsonClient {
    val address = InetSocketAddress(host, port)
    val transceiver = JsonTransceiver(port)
    return createClient(address, createClientConfig(transceiver))
  }

  override fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig,
    vararg args: String
  ): JsonClient {
    val transceiver = config.transceiver as JsonTransceiver
    val bootstrap = config.bootstrap
    val clientChannel = JsonClientChannel(transceiver)
    bootstrap.handler(clientChannel)
    val client = JsonClient(address, config)
    clientArrayList.add(client)
    return client
  }

  override fun shutdown() {
    for (client in clientArrayList) {
      client.shutdown()
    }
    clientArrayList.clear()
    workerGroup.shutdownGracefully()
  }
}
