package konem

import konem.netty.*
import konem.netty.client.ClientFactoryConfig
import konem.netty.client.TcpSocketClientFactory
import konem.netty.server.ServerConfig
import konem.netty.server.TcpSocketServer
import konem.netty.server.WebSocketServer
import konem.netty.server.WebSocketServerConfig
import konem.protocol.tcp.TcpClientFactory
import konem.protocol.tcp.TcpSocketServerImp
import konem.protocol.websocket.WebSocketServerImp

class Konem private constructor() {
    companion object {

        fun <T> createTcpSocketServer(
            config: ServerConfig.() -> Unit,
            heartbeatProtocol: ServerHeartbeatProtocol<T>,
            protocolPipeline: ProtocolPipeline<T>
        ): TcpSocketServer<T> {
            val userConfig = ServerConfig()
            config(userConfig)
            val server = TcpSocketServerImp(userConfig, heartbeatProtocol, protocolPipeline)
            for (port in userConfig.portSet) {
                server.addChannel(port)
            }
            return server
        }

        fun <T> createTcpSocketClientFactoryOfDefaults(
            heartbeatProtocol: ClientHeartbeatProtocol,
            protocolPipeline: ProtocolPipeline<T>
            ): TcpSocketClientFactory<T> {
            return TcpClientFactory(ClientFactoryConfig(),heartbeatProtocol,protocolPipeline)
        }

        fun <T> createWebSocketServer(
            config: (WebSocketServerConfig) -> Unit,
            heartbeatProtocol: ServerHeartbeatProtocol<T>,
            protocolPipeline: ProtocolPipeline<T>
        ): WebSocketServer<T> {
            val userConfig = WebSocketServerConfig()
            config(userConfig)
            val server = WebSocketServerImp(userConfig, heartbeatProtocol, protocolPipeline)
            for((port, websockets) in userConfig.portToWsMap){
                server.addChannel(port,*websockets)
            }
            return server
        }

    }


}


