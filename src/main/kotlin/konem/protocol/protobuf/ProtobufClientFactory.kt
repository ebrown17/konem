package konem.protocol.protobuf

import konem.netty.stream.client.ClientBootstrapConfig
import konem.netty.stream.client.ClientFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.ArrayList

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class ProtobufClientFactory : ClientFactory() {
  private val logger = LoggerFactory.getLogger(ProtobufClientFactory::class.java)
  private val clientArrayList = ArrayList<ProtobufClient>()

  override fun createClient(
    host: String,
    port: Int,
    vararg args: String
  ): ProtobufClient {
    val address = InetSocketAddress(host, port)
    val transceiver = ProtobufTransceiver(port)
    return createClient(address, createClientConfig(transceiver))
  }

  override fun createClient(
    address: InetSocketAddress,
    config: ClientBootstrapConfig,
    vararg args: String
  ): ProtobufClient {
    val transceiver = config.transceiver as ProtobufTransceiver
    val bootstrap = config.bootstrap
    val clientChannel = ProtobufClientChannel(transceiver)
    bootstrap.handler(clientChannel)
    val client = ProtobufClient(address, config)
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
