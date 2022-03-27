package konem.protocol.konem.wire

import konem.data.protobuf.KonemMessage
import konem.netty.tcp.client.Client
import konem.netty.tcp.client.ClientBootstrapConfig
import konem.netty.tcp.client.ClientFactory
import konem.netty.tcp.client.ClientFactoryConfig

import java.net.InetSocketAddress

class WireClientFactory private constructor(config: ClientFactoryConfig) : ClientFactory<KonemMessage>(config) {

    companion object {

        fun createDefault(): WireClientFactory{
            return WireClientFactory(ClientFactoryConfig())
        }

        fun create(config: (ClientFactoryConfig) -> Unit): WireClientFactory {
            val userConfig = ClientFactoryConfig()
            config(userConfig)
            return WireClientFactory(userConfig)
        }
    }


    override fun createClient(host: String, port: Int, vararg args: String): Client<KonemMessage> {
        val address = InetSocketAddress(host, port)
        val transceiver = WireTransceiver(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<KonemMessage>,
        vararg args: String
    ): Client<KonemMessage> {
        val transceiver = config.transceiver
        val bootstrap = config.bootstrap
        val client = WireClient(address, config)
        val clientChannel = WireClientChannel(transceiver as WireTransceiver,config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
