package konem.protocol.tcp

import konem.data.json.KonemMessage
import konem.netty.client.Client
import konem.netty.client.ClientBootstrapConfig
import konem.netty.client.ClientFactory
import konem.netty.client.ClientFactoryConfig

import java.net.InetSocketAddress

class TcpClientFactory<I> private constructor(config: ClientFactoryConfig) : ClientFactory<I>(config) {

/*    companion object {

        fun createDefault(): JsonClientFactory{
            return JsonClientFactory(ClientFactoryConfig())
        }

        fun create(config: (ClientFactoryConfig) -> Unit): JsonClientFactory {
            val userConfig = ClientFactoryConfig()
            config(userConfig)
            return JsonClientFactory(userConfig)
        }
    }*/


    override fun createClient(host: String, port: Int, vararg args: String): Client<I> {
        val address = InetSocketAddress(host, port)
        val transceiver = TcpTransceiver<I>(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<I>,
        vararg args: String
    ): Client<I> {
        val transceiver = config.transceiver
        val bootstrap = config.bootstrap
        val client = TcpClient(address, config)
        val clientChannel = TcpClientChannel(transceiver as TcpTransceiver,config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
