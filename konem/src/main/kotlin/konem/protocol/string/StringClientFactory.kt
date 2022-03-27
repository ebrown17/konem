package konem.protocol.string

import konem.netty.tcp.client.Client
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientFactory
import konem.netty.tcp.client.ClientFactoryConfig

import java.net.InetSocketAddress

class StringClientFactory private constructor(config: ClientFactoryConfig) : ClientFactory<String>(config) {

    companion object {

        fun createDefault(): StringClientFactory{
            return StringClientFactory(ClientFactoryConfig())
        }

        fun create(config: (ClientFactoryConfig) -> Unit): StringClientFactory {
            val userConfig = ClientFactoryConfig()
            config(userConfig)
            return StringClientFactory(userConfig)
        }
    }


    override fun createClient(host: String, port: Int, vararg args: String): Client<String> {
        val address = InetSocketAddress(host, port)
        val transceiver = StringTransceiver(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<String>,
        vararg args: String
    ): Client<String> {
        val transceiver = config.transceiver as StringTransceiver
        val bootstrap = config.bootstrap
        val client = StringClient(address, config)
        val clientChannel = StringClientChannel(transceiver,config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}