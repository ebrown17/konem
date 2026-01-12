package konem.netty.stream

import java.net.SocketAddress

interface HandlerListener<H, T> {
    fun registerActiveHandler(handler: Handler<H, T>, channelPort: Int, remoteConnection: SocketAddress)
    fun registerInActiveHandler(handler: Handler<H, T>, channelPort: Int, remoteConnection: SocketAddress)
}
