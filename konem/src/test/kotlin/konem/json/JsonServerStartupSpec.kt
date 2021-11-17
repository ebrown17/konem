package konem.json

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import konem.protocol.socket.json.JsonClient
import konem.protocol.socket.json.JsonClientFactory
import konem.protocol.socket.json.JsonServer
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalKotest
class JsonServerStartupSpec : ShouldSpec({

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    should(": Server starts with expected ports and values") {
        forAll(
            row(1, arrayOf(6060)),
            row(3, arrayOf(6060,6061,6062)),
        ) { totalConfigured, ports ->

            clientFactory?.shutdown()
            server?.shutdownServer()

            server = JsonServer()
            server?.let { srv ->
                ports.forEach { port ->
                    srv.addChannel(port)
                }
            }

            startServer()
            delay(Duration.seconds(1))
            until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
                if (server != null) {
                    var allPortsConfigured = true

                    ports.forEach { port ->
                        if (!server!!.isPortConfigured(port)) {
                            allPortsConfigured = false
                        }
                    }

                    val transceiverMap = server!!.getTransceiverMap()
                    var allTransceiversConfigured = true

                    transceiverMap.forEach { (port, _) ->
                        if (!ports.contains(port)) {
                            allTransceiversConfigured = false
                        }
                    }

                    print("Ports Configured: $allPortsConfigured | ")
                    print("Transceivers Configured: $allTransceiversConfigured | ")
                    println("TransceiverMap Size: ${transceiverMap.size} Expected:  $totalConfigured")

                    allPortsConfigured  && allTransceiversConfigured && (transceiverMap.size == totalConfigured)

                } else {
                    false
                }
            }
            if (DEBUG) println("-----------------------------------")
        }
    }
})
