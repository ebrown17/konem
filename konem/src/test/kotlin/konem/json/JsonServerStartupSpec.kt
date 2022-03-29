package konem.json

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import konem.protocol.konem.json.JsonServer
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
            nameFn = { data: ServerStartup -> "${this.testCase.displayName} ${data.portsToConfigure}" },
            ServerStartup( mutableListOf(6060)),
            ServerStartup( mutableListOf(6060,6061,6062)),
            ServerStartup( mutableListOf(6060,6061,6062,6063,6064,6065)),
            ServerStartup( mutableListOf(6060,6061,6062,6060,6061,6062)),
            ServerStartup( mutableListOf(6060,6061,6062,6061,6062,6065)),

        ) { ( portsToConfigure) ->

            server = JsonServer.create { serverConfig ->
                portsToConfigure.forEach { port ->
                    serverConfig.addChannel(port)
                }
            }

            startServer()
            delay(Duration.seconds(1))
            until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
                if (server != null) {
                    var allPortsConfigured = true

                    portsToConfigure.forEach { port ->
                        if (!server!!.isActive(port)) {
                            allPortsConfigured = false
                        }
                    }

                    println("Ports Configured: $allPortsConfigured | ")

                    allPortsConfigured
                } else {
                    false
                }
            }
            if (DEBUG) println("-----------------------------------")
        }
    }
})
