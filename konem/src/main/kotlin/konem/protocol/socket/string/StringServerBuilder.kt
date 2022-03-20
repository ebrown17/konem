package konem.protocol.socket.string

import konem.netty.tcp.server.Server
import konem.netty.tcp.server.ServerBuilder
import konem.netty.tcp.server.ServerConfig

class StringServerBuilder: ServerBuilder<String>() {
    override fun createServer(serverConfig: ServerConfig): Server<String> {
        return StringServer(serverConfig)
    }
}
