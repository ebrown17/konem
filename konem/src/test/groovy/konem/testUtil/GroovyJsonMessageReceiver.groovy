package konem.testUtil

import com.sun.istack.internal.NotNull
import konem.data.json.KonemMessage
import konem.protocol.socket.json.KonemMessageReceiver
import kotlin.Unit
import kotlin.jvm.functions.Function2

class GroovyJsonMessageReceiver extends KonemMessageReceiver {
    int messageCount = 0
    def messageList = []
    def clientId = ""
    GroovyJsonMessageReceiver(@NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
    }

    GroovyJsonMessageReceiver(String clientId, @NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
        this.clientId = clientId
    }
}