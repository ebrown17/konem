package konem.protocol.websocket

import konem.netty.ClientHeartbeatProtocol
import konem.netty.ProtocolPipeline
import konem.netty.client.*

import java.net.InetSocketAddress

class WebSocketClientFactoryImp<T> internal constructor(
    config: ClientFactoryConfig,
    heartbeatProtocol: ClientHeartbeatProtocol,
    protocolPipeline: ProtocolPipeline<T>
) : ClientFactory<T>(config, heartbeatProtocol, protocolPipeline), WebSocketClientFactory<T> {

    override fun createClient(host: String, port: Int,vararg args: String): Client<T> {
        val address = InetSocketAddress(host, port)
        val transceiver = WebSocketTransceiver<T>(port)
        return createClient(address, createClientConfig(transceiver))
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<T>,
        vararg args: String
    ): Client<T> {
        val transceiver = config.transceiver as WebSocketTransceiver<T>
        val bootstrap = config.bootstrap
        val client = WebSocketClient(address, config)
        val clientChannel = WebSocketClientChannel(transceiver, config.clientChannelInfo)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

}
