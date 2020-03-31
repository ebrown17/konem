package konem.protocol.socket.wire

import java.net.InetSocketAddress
import java.util.ArrayList
import konem.data.protobuf.KonemMessage
import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientFactory
import org.slf4j.LoggerFactory

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class WireClientFactory : ClientFactory<KonemMessage, KonemMessage>() {
  private val logger = LoggerFactory.getLogger(WireClientFactory::class.java)
  private val clientArrayList = ArrayList<WireClient>()

  override fun createClient(
    host: String,
    port: Int,
    vararg args: String
  ): WireClient {
    val address = InetSocketAddress(host, port)
    val transceiver = WireTransceiver(port)
    return createClient(address, createClientConfig(transceiver))
  }

  override fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig<KonemMessage, KonemMessage>,
    vararg args: String
  ): WireClient {
    val transceiver = config.transceiver as WireTransceiver
    val bootstrap = config.bootstrap
    val clientChannel = WireClientChannel(transceiver)
    bootstrap.handler(clientChannel)
    val client = WireClient(address, config)
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
