package konem.protocol.socket.string


import konem.netty.tcp.Receiver
import java.net.SocketAddress

class StringMessageReceiver(private val received: (SocketAddress, String) -> Unit):
    Receiver<String>() {

    override fun receive(addr: SocketAddress, message: String) {
        synchronized(this) {
            received(addr, message)
        }
    }
}
