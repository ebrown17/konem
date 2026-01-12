package konem.netty.stream

import java.net.SocketAddress

interface Receiver {
    fun handle(addr: SocketAddress, msg: Any)
}
