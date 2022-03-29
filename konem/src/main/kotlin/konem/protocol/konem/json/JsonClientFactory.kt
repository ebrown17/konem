package konem.protocol.konem.json

import konem.data.json.KonemMessage
import konem.netty.tcp.client.Client
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientFactory
import konem.netty.tcp.client.ClientFactoryConfig

import java.net.InetSocketAddress

class JsonClientFactory private constructor(config: ClientFactoryConfig) : ClientFactory<KonemMessage>(config) {

    companion object {

        fun createDefault(): JsonClientFactory{
            return JsonClientFactory(ClientFactoryConfig())
        }

        fun create(config: (ClientFactoryConfig) -> Unit): JsonClientFactory {
            val userConfig = ClientFactoryConfig()
            config(userConfig)
            return JsonClientFactory(userConfig)
        }
    }


    override fun createClient(host: String, port: Int, vararg args: String): Client<KonemMessage> {
        val address = InetSocketAddress(host, port)
        val transceiver = JsonTransceiver(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<KonemMessage>,
        vararg args: String
    ): Client<KonemMessage> {
        val transceiver = config.transceiver
        val bootstrap = config.bootstrap
        val client = JsonClient(address, config)
        val clientChannel = JsonClientChannel(transceiver as JsonTransceiver,config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
