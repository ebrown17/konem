package konem

import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import konem.data.protobuf.HeartBeat
import konem.netty.*
import konem.netty.client.ClientFactoryConfig
import konem.netty.client.TcpSocketClientFactory
import konem.netty.client.WebSocketClientFactory
import konem.netty.server.ServerConfig
import konem.netty.server.TcpSocketServer
import konem.netty.server.WebSocketServer
import konem.netty.server.WebSocketServerConfig
import konem.protocol.tcp.TcpClientFactory
import konem.protocol.tcp.TcpSocketServerImp
import konem.protocol.websocket.WebSocketClientFactoryImp
import konem.protocol.websocket.WebSocketServerImp

class Konem private constructor() {
    companion object {

        fun <T> createTcpSocketServer(
            config: ServerConfig.() -> Unit,
            protocolPipeline: ProtocolPipeline<T>,
            heartbeatProtocol: ServerHeartbeatProtocol,
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
            protocolPipeline: ProtocolPipeline<T>,
            heartbeatProtocol: ClientHeartbeatProtocol,
            ): TcpSocketClientFactory<T> {
            return TcpClientFactory(ClientFactoryConfig(),heartbeatProtocol,protocolPipeline)
        }

        fun <T> createWebSocketServer(
            config: WebSocketServerConfig.() -> Unit,
            protocolPipeline: ProtocolPipeline<T>
        ): WebSocketServer<T> {
            val userConfig = WebSocketServerConfig()
            config(userConfig)
            val server = WebSocketServerImp(
                userConfig,
                ServerHeartbeatProtocol(true,10) { PingWebSocketFrame() },
                protocolPipeline
            )
            for((port, websockets) in userConfig.portToWsMap){
                server.addChannel(port,*websockets.toTypedArray())
            }
            return server
        }

        fun <T> createWebSocketClientFactoryOfDefaults(
            protocolPipeline: ProtocolPipeline<T>
        ): WebSocketClientFactory<T> {
            return WebSocketClientFactoryImp(
                ClientFactoryConfig(),
                protocolPipeline,
                heartbeatProtocol = ClientHeartbeatProtocol(dropHeartbeat=false, isHeartbeat = { message ->
                    message is WebSocketFrame
                })
            )
        }
    }
}


