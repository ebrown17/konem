package konem.protocol.konem

import konem.data.json.KonemMessage
import konem.netty.Receiver
import java.net.SocketAddress

open class KonemJsonMessageReceiver (private val received: (SocketAddress, konem.data.json.KonemMessage) -> Unit):
    Receiver<KonemMessage>() {

    override fun receive(addr: SocketAddress, message: konem.data.json.KonemMessage) {
        synchronized(this) {
            received(addr, message)
        }
    }
}

class KonemWireMessageReceiver (private val received: (SocketAddress, konem.data.protobuf.KonemMessage) -> Unit):
    Receiver<konem.data.protobuf.KonemMessage>() {

    override fun receive(addr: SocketAddress, message: konem.data.protobuf.KonemMessage) {
        synchronized(this) {
            received(addr, message)
        }
    }
}
