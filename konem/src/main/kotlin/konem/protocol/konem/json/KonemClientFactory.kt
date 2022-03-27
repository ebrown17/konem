package konem.protocol.konem.json

import konem.data.json.KonemMessage
import konem.netty.tcp.client.Client
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientFactory
import konem.netty.tcp.client.ClientFactoryConfig

import java.net.InetSocketAddress

class KonemClientFactory private constructor(config: ClientFactoryConfig) : ClientFactory<KonemMessage>(config) {

    companion object {

        fun createDefault(): KonemClientFactory{
            return KonemClientFactory(ClientFactoryConfig())
        }

        fun create(config: (ClientFactoryConfig) -> Unit): KonemClientFactory {
            val userConfig = ClientFactoryConfig()
            config(userConfig)
            return KonemClientFactory(userConfig)
        }
    }


    override fun createClient(host: String, port: Int, vararg args: String): Client<KonemMessage> {
        val address = InetSocketAddress(host, port)
        val transceiver = KonemTransceiver(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<KonemMessage>,
        vararg args: String
    ): Client<KonemMessage> {
        val transceiver = config.transceiver
        val bootstrap = config.bootstrap
        val client = KonemClient(address, config)
        val clientChannel = KonemClientChannel(transceiver as KonemTransceiver,config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
