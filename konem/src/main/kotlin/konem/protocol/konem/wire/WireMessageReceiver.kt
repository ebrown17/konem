package konem.protocol.konem.wire


import konem.data.protobuf.KonemMessage
import konem.netty.tcp.Receiver
import java.net.SocketAddress

class WireMessageReceiver(private val received: (SocketAddress, KonemMessage) -> Unit):
    Receiver<KonemMessage>() {

    override fun receive(addr: SocketAddress, message: KonemMessage) {
        synchronized(this) {
            received(addr, message)
        }
    }
}
