package konem.wire

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

import konem.*
import konem.data.protobuf.HeartBeat
import konem.data.protobuf.MessageType
import konem.netty.ServerHeartbeatProtocol
import konem.protocol.konem.KonemProtocolPipeline
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalKotest
class WireServerStartupSpec : FunSpec({
    afterTest{
        clientFactory?.shutdown()
        server?.shutdownServer()
    }
    context("Server starts with expected ports and values") {
        withData(
            nameFn = { data: ServerStartup -> "${this.testCase.name.testName} ${data.portsToConfigure}" },
            ServerStartup(mutableListOf(6060)),
            ServerStartup(mutableListOf(6060, 6061, 6062)),
            ServerStartup(mutableListOf(6060, 6061, 6062, 6063, 6064, 6065)),
            ServerStartup(mutableListOf(6060, 6061, 6062, 6060, 6061, 6062)),
            ServerStartup(mutableListOf(6060, 6061, 6062, 6061, 6062, 6065)),

            ) { (portsToConfigure) ->

            server = Konem.createTcpSocketServer(
                config = {
                    portsToConfigure.forEach { port ->
                        addChannel(port)
                    }
                },
                protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline(),
                heartbeatProtocol = ServerHeartbeatProtocol {
                    konem.data.protobuf.KonemMessage(
                        messageType = MessageType.HEARTBEAT,
                        heartBeat = HeartBeat(Date().toString())
                    )
                },
            )

            startServer(server!!)
            delay(1.seconds)
            until(activeTime.seconds, 250.milliseconds.fixed()) {
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
