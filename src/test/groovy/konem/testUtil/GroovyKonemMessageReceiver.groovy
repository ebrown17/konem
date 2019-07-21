package konem.testUtil

import konem.data.json.KonemMessage
import konem.protocol.websocket.KonemMessageReceiver
import kotlin.Unit
import kotlin.jvm.functions.Function2
import org.jetbrains.annotations.NotNull

class GroovyKonemMessageReceiver extends KonemMessageReceiver {
    int messageCount = 0
    def messageList = []
    def clientId = ""
    GroovyKonemMessageReceiver(@NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
    }

    GroovyKonemMessageReceiver(String clientId, @NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
        this.clientId = clientId
    }
}
