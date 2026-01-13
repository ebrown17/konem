package konem.jsonnew

import io.kotest.common.ExperimentalKotest
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import konem.ClientCommConfigsV1
import konem.ClientConfig
import konem.DEBUG
import konem.Konem
import konem.connectClients
import konem.jsonnew.startServerWithWait
import konem.jsonnew.waitForMessagesServerLong
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.json.JsonTestClientReceiver
import konem.json.JsonTestServerReceiver
import konem.json.sendClientMessages
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ServerHeartbeatProtocol
import konem.netty.client.Client
import konem.netty.client.TcpSocketClientFactory
import konem.netty.server.TcpSocketServer
import konem.protocol.konem.KonemProtocolPipeline
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalKotest::class)
class JsonNewCommunicationSpec : FunSpec({
    testExecutionMode = TestExecutionMode.Sequential
    var server: TcpSocketServer<KonemMessage>? = null
    var clientFactory: TcpSocketClientFactory<KonemMessage>? = null

    beforeTest {
        clientFactory?.shutdown()
        server?.shutdownServer()

        server = Konem.createTcpSocketServer(
            config = {
                addChannel(6060)
                addChannel(6061)
                addChannel(6062)
                addChannel(6063)
            },
            protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
            heartbeatProtocol = ServerHeartbeatProtocol { KonemMessage(Heartbeat()) },
        )

        clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
            protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline(),
            heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
                message is Heartbeat
            }),
        )
    }

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    context(": Server readers can register and then see messages: ") {
        withTests(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}" },
            ts = listOf(
                ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
                ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 5))),
                ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 5))),
                ClientCommConfigsV1(15, mutableListOf(ClientConfig(6060, 2), ClientConfig(6061, 5))),
                ClientCommConfigsV1(25, mutableListOf(
                    ClientConfig(6060, 2), ClientConfig(6061, 5),
                    ClientConfig(6062, 8)
                )),
            ),
        ) { (msgCount, clientConfigs) ->
            blockingTest = true
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()

            serverReceiver = JsonTestServerReceiver { _, _ ->
                serverReceiver.messageCount++
            }
            serverReceiverList.add(serverReceiver)

            clientConfigs.forEach { config ->
                server?.registerChannelMessageReceiver(config.port, serverReceiver)
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost", config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, _ ->
                            clientReceiver.messageCount++
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        it.registerChannelMessageReceiver(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServerWithWait(server!!)
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount, clientList)
            waitForMessagesServerLong(totalMessagesSent, serverReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }
})
