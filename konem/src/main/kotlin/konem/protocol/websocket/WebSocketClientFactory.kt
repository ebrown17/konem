package konem.protocol.websocket

import konem.netty.ClientHeartbeatProtocol
import konem.netty.ProtocolPipeline
import konem.netty.client.*

import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException

class WebSocketClientFactoryImp<T> internal constructor(
    config: ClientFactoryConfig,
    protocolPipeline: ProtocolPipeline<T>,
    heartbeatProtocol: ClientHeartbeatProtocol
) : ClientFactory<T>(config, heartbeatProtocol, protocolPipeline), WebSocketClientFactory<T> {

    override fun createClient(host: String, port: Int,webSocketPath: String): WebSocketClient<T> {
        val address = InetSocketAddress(host, port)
        val transceiver = WebSocketTransceiver<T>(port)
        return createClient(address, createClientConfig(transceiver),webSocketPath)
    }

    override fun createClient(
        address: InetSocketAddress,
        config: ClientBootstrapConfig<T>,
        webSocketPath: String
    ): WebSocketClient<T> {
        val fullWebSocketUrl = buildFullWebSocketPath(address, webSocketPath)
        val transceiver = config.transceiver as WebSocketTransceiver<T>
        val bootstrap = config.bootstrap
        val client = WebSocketClientImp(address, config,fullWebSocketUrl)
        val clientChannel = WebSocketClientChannel(transceiver, config.clientChannelInfo, fullWebSocketUrl)
        bootstrap.handler(clientChannel)
        clientArrayList.add(client)
        return client
    }

    @Throws(URISyntaxException::class)
    private fun buildFullWebSocketPath(address: InetSocketAddress, webSocketPath: String): URI {
        return URI("ws://" + address.hostString + ":" + address.port + webSocketPath)
    }
}
