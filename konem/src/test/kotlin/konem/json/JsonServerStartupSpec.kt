package konem.json

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.datatest.withData
import konem.protocol.socket.json.JsonServer
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalKotest
class JsonServerStartupSpec : ShouldSpec({

    afterContainer {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    should(": Server starts with expected ports and values: ") {
        withData(
            nameFn = { data: ServerStartup -> "${this.testCase.displayName} ${data.totalConfigured} ${data.portsToConfigure}" },
            ServerStartup(1, mutableListOf(6060)),
            ServerStartup(3, mutableListOf(6060,6061,6062)),
            ServerStartup(6, mutableListOf(6060,6061,6062,6063,6064,6065)),
            ServerStartup(3, mutableListOf(6060,6061,6062,6060,6061,6062)),
            ServerStartup(4, mutableListOf(6060,6061,6062,6061,6062,6065)),

        ) { (totalConfigured, portsToConfigure) ->

            server = JsonServer()
            server?.let { srv ->
                portsToConfigure.forEach { port ->
                    srv.addChannel(port)
                }
            }

            startServer()
            delay(Duration.seconds(1))
            until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
                if (server != null) {
                    var allPortsConfigured = true

                    portsToConfigure.forEach { port ->
                        if (!server!!.isPortConfigured(port)) {
                            allPortsConfigured = false
                        }
                    }

                    val transceiverMap = server!!.getTransceiverMap()
                    var allTransceiversConfigured = true

                    transceiverMap.forEach { (port, _) ->
                        if (!portsToConfigure.contains(port)) {
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
