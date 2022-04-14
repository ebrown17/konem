package konem.wire

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import konem.*
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.data.protobuf.HeartBeat
import konem.data.protobuf.MessageType
import konem.netty.ServerHeartbeatProtocol
import konem.protocol.konem.KonemProtocolPipeline
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalKotest
class WireServerStartupSpec : ShouldSpec({

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

            server = Konem.createTcpSocketServer(
                config = {
                    portsToConfigure.forEach { port ->
                        it.addChannel(port)
                    }
                },
                heartbeatProtocol = ServerHeartbeatProtocol {
                    konem.data.protobuf.KonemMessage(
                        messageType = MessageType.HEARTBEAT,
                        heartBeat = HeartBeat(Date().toString())
                    )
                },
                protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline()
            )

            startServer(server!!)
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
