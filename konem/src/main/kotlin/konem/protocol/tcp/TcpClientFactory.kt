package konem.protocol.tcp

import konem.netty.ClientHeartbeatProtocol
import konem.netty.ProtocolPipeline
import konem.netty.client.Client
import konem.netty.client.ClientBootstrapConfig
import konem.netty.client.ClientFactory
import konem.netty.client.ClientFactoryConfig

import java.net.InetSocketAddress

class TcpClientFactory<T> internal constructor(
    config: ClientFactoryConfig,
    heartbeatProtocol: ClientHeartbeatProtocol,
    protocolPipeline: ProtocolPipeline<T>
) : ClientFactory<T>(config, heartbeatProtocol, protocolPipeline) {

    override fun createClient(host: String, port: Int, vararg args: String): Client<T> {
        val address = InetSocketAddress(host, port)
        val transceiver = TcpTransceiver<T>(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<T>,
        vararg args: String
    ): Client<T> {
        val transceiver = config.transceiver as TcpTransceiver<T>
        val bootstrap = config.bootstrap
        val client = TcpClient(address, config)
        val clientChannel = TcpClientChannel(transceiver, config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
