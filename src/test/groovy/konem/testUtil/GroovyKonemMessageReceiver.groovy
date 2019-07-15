package konem.testUtil

import konem.data.json.KonemMessage
import konem.protocol.websocket.KonemMessageReceiver
import kotlin.Unit
import kotlin.jvm.functions.Function2
import org.jetbrains.annotations.NotNull

class GroovyKonemMessageReceiver extends KonemMessageReceiver {
    int messageCount = 0
    GroovyKonemMessageReceiver(@NotNull Function2<? super InetSocketAddress, ? super KonemMessage, Unit> receive) {
        super(receive)
    }
}
