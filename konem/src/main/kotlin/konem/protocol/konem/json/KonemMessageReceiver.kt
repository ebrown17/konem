package konem.protocol.konem.json


import konem.data.json.KonemMessage
import konem.netty.tcp.Receiver
import java.net.SocketAddress

class KonemMessageReceiver(private val received: (SocketAddress, KonemMessage) -> Unit):
    Receiver<KonemMessage>() {

    override fun receive(addr: SocketAddress, message: KonemMessage) {
        synchronized(this) {
            received(addr, message)
        }
    }
}
