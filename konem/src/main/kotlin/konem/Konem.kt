package konem

import konem.data.json.KonemMessage
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ProtocolPipeline
import konem.netty.ServerHeartbeatProtocol
import konem.netty.client.ClientFactoryConfig
import konem.netty.server.Server
import konem.netty.server.ServerConfig
import konem.protocol.tcp.TcpClientFactory
import konem.protocol.tcp.TcpServer

class Konem private constructor() {
    companion object {

        fun <T> createTcpServer(
            config: (ServerConfig) -> Unit,
            heartbeatProtocol: ServerHeartbeatProtocol<T>,
            protocolPipeline: ProtocolPipeline<T>
        ): Server<T> {
            val userConfig = ServerConfig()
            config(userConfig)
            val server = TcpServer(userConfig, heartbeatProtocol, protocolPipeline)
            for (port in userConfig.portSet) {
                server.addChannel(port)
            }
            return server
        }

        fun <T> createTcpServerNoHeartbeat(
            config: (ServerConfig) -> Unit,
            protocolPipeline: ProtocolPipeline<T>
        ): Server<T> {
            val userConfig = ServerConfig()
            config(userConfig)
            val heartbeatProtocol =  ServerHeartbeatProtocol(false, generateHeartBeat =  { Any() as T } )
            val server = TcpServer(userConfig, heartbeatProtocol, protocolPipeline)
            for (port in userConfig.portSet) {
                server.addChannel(port)
            }
            return server
        }


        fun <T> createClientFactoryOfDefaults(
            heartbeatProtocol: ClientHeartbeatProtocol<T>,
            protocolPipeline: ProtocolPipeline<T>
            ): TcpClientFactory<T> {
            return TcpClientFactory(ClientFactoryConfig(),heartbeatProtocol,protocolPipeline)
        }

    }


}


