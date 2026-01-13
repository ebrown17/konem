package konem.jsonnew

import io.kotest.assertions.nondeterministic.until
import io.kotest.common.ExperimentalKotest
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import konem.Konem
import konem.ServerStartup
import konem.activeTime
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.ServerHeartbeatProtocol
import konem.netty.client.TcpSocketClientFactory
import konem.netty.server.TcpSocketServer
import konem.protocol.konem.KonemProtocolPipeline
import konem.jsonnew.startServerWithWait
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalKotest::class)
class JsonNewServerStartupSpec : FunSpec({
    testExecutionMode = TestExecutionMode.Sequential
    var server: TcpSocketServer<KonemMessage>? = null
    var clientFactory: TcpSocketClientFactory<KonemMessage>? = null

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    context(": Server starts with expected ports and values: ") {
        withTests(
            nameFn = { data: ServerStartup -> "${this.testCase.name.name} ${data.portsToConfigure}" },
            ts = listOf(
                ServerStartup(mutableListOf(6060)),
                ServerStartup(mutableListOf(6060, 6061, 6062)),
                ServerStartup(mutableListOf(6060, 6061, 6062, 6063)),
            ),
        ) { (portsToConfigure) ->
            blockingTest = true
            server = Konem.createTcpSocketServer(
                config = {
                    portsToConfigure.forEach { port ->
                        addChannel(port)
                    }
                },
                protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
                heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) }
            )

            startServerWithWait(server!!)
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
        }
    }
})
