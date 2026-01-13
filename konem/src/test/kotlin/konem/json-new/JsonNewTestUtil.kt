package konem.jsonnew

import io.kotest.assertions.nondeterministic.until
import konem.TestServerReceiver
import konem.netty.server.Server
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private const val jsonNewActiveTimeSeconds = 20
private const val jsonNewWaitForMsgTimeSeconds = 20

@OptIn(ExperimentalTime::class)
suspend fun <T> startServerWithWait(server: Server<T>): Boolean {
    server.startServer()
    until(jsonNewActiveTimeSeconds.seconds) {
        server.allActive()
    }
    return true
}

@OptIn(ExperimentalTime::class)
suspend fun <T> waitForMessagesServerLong(
    totalMessages: Int,
    receiverList: MutableList<out TestServerReceiver<T>>,
    debug: Boolean = false,
): Boolean {
    var waitCount = 1
    until(jsonNewWaitForMsgTimeSeconds.seconds) {
        val received: Int = receiverList.sumOf { it.messageCount }
        if (debug) {
            println("Server received: $received out of $totalMessages (check ${waitCount++})")
        }
        received == totalMessages
    }
    return true
}
