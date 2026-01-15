package konem.json

import io.kotest.assertions.nondeterministic.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withTests
import io.kotest.engine.concurrency.TestExecutionMode
import konem.*
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.ServerHeartbeatProtocol
import konem.protocol.konem.KonemProtocolPipeline
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalKotest
class JsonServerStartupSpec : FunSpec({
    testExecutionMode = TestExecutionMode.Sequential

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
        delay(500.milliseconds)
    }

     context(": Server starts with expected ports and values: ") {
        withTests(
            nameFn = { data: ServerStartup -> "${this.testCase.name.name} ${data.portsToConfigure}" },
            ts = listOf(
            ServerStartup( mutableListOf(6060)),
            ServerStartup( mutableListOf(6060,6061,6062)),
            ServerStartup( mutableListOf(6060,6061,6062,6063,6064,6065)),
            ServerStartup( mutableListOf(6060,6061,6062,6060,6061,6062)),
            ServerStartup( mutableListOf(6060,6061,6062,6061,6062,6065)),

            ),
        ) { ( portsToConfigure) ->

            server = Konem.createTcpSocketServer(
                config = {
                    portsToConfigure.forEach { port ->
                        addChannel(port)
                    }
                },
                protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
                heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) }
            )


            startServer(server!!)
            delay(1.seconds)
            until(activeTime.seconds) {
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
