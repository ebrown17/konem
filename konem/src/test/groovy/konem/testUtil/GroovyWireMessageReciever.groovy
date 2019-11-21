package konem.testUtil

import konem.data.protobuf.KonemMessage
import konem.protocol.socket.wire.WireMessageReceiver
import kotlin.Unit
import kotlin.jvm.functions.Function2
import org.jetbrains.annotations.NotNull

class GroovyWireMessageReciever extends WireMessageReceiver {
    int messageCount = 0
    def messageList = []
    def clientId = ""
    GroovyWireMessageReciever(@NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
    }

    GroovyWireMessageReciever(String clientId, @NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
        this.clientId = clientId
    }
}

