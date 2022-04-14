package konem

import konem.netty.*
import konem.netty.client.ClientFactoryConfig
import konem.netty.server.ServerConfig
import konem.netty.server.TcpSocketServer
import konem.protocol.tcp.TcpClientFactory
import konem.protocol.tcp.TcpServer

class Konem private constructor() {
    companion object {

        fun <T> createTcpSocketServer(
            config: (ServerConfig) -> Unit,
            heartbeatProtocol: ServerHeartbeatProtocol<T>,
            protocolPipeline: ProtocolPipeline<T>
        ): TcpSocketServer<T> {
            val userConfig = ServerConfig()
            config(userConfig)
            val server = TcpServer(userConfig, heartbeatProtocol, protocolPipeline)
            for (port in userConfig.portSet) {
                server.addChannel(port)
            }
            return server
        }

        fun <T> createTcpSocketClientFactoryOfDefaults(
            heartbeatProtocol: ClientHeartbeatProtocol,
            protocolPipeline: ProtocolPipeline<T>
            ): TcpClientFactory<T> {
            return TcpClientFactory(ClientFactoryConfig(),heartbeatProtocol,protocolPipeline)
        }


    }


}


