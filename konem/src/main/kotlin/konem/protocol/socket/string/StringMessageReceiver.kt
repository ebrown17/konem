package konem.protocol.socket.string


import konem.logger
import konem.netty.tcp.Receiver
import java.net.SocketAddress

class StringMessageReceiver(private val receive: (SocketAddress, String) -> Unit):
    Receiver<String>() {
    private val logger = logger(this)

    override fun receive(addr: SocketAddress, message: String) {
        synchronized(this) {
            receive(addr, message)
        }
    }
}
