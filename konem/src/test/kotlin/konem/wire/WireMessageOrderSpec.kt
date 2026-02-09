package konem.wire

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.matchers.shouldBe
import konem.ClientCommConfigsV1
import konem.ClientConfig
import konem.DEBUG
import konem.Konem
import konem.connectClients
import konem.data.protobuf.Data
import konem.data.protobuf.HeartBeat
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.ClientHeartbeatProtocol
import konem.netty.ServerHeartbeatProtocol
import konem.netty.client.Client
import konem.protocol.konem.KonemProtocolPipeline
import konem.startServer
import konem.waitForMessagesClient
import konem.waitForMessagesServer
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class WireMessageOrderSpec : FunSpec({
    testExecutionMode = TestExecutionMode.Sequential

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
            protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline(),
            heartbeatProtocol = ServerHeartbeatProtocol {
                KonemMessage(
                    messageType = MessageType.HEARTBEAT,
                    heartBeat = HeartBeat(Date().toString())
                )
            },
        )

        clientFactory = Konem.createTcpSocketClientFactoryOfDefaults(
            protocolPipeline = KonemProtocolPipeline.getKonemWirePipeline(),
            heartbeatProtocol = ClientHeartbeatProtocol(isHeartbeat = { message ->
                message is KonemMessage && message.messageType == MessageType.HEARTBEAT
            }),
        )
    }

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
        delay(500.milliseconds)
    }

    context(":Messages sent by client are received in expected order") {
        withTests(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}" },
            ts = listOf(
                ClientCommConfigsV1(1500, mutableListOf(ClientConfig(6060, 1))),
                ClientCommConfigsV1(1000, mutableListOf(ClientConfig(6060, 5))),
                ClientCommConfigsV1(1000, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 1))),
                ClientCommConfigsV1(1000, mutableListOf(ClientConfig(6060, 5), ClientConfig(6061, 5))),
                ClientCommConfigsV1(
                    1000,
                    mutableListOf(
                        ClientConfig(6060, 1),
                        ClientConfig(6061, 1),
                        ClientConfig(6062, 1),
                        ClientConfig(6063, 1),
                    )
                ),
                ClientCommConfigsV1(
                    750,
                    mutableListOf(
                        ClientConfig(6060, 5),
                        ClientConfig(6061, 5),
                        ClientConfig(6062, 5),
                        ClientConfig(6063, 5),
                    )
                )
            ),
        ) { (msgCount, clientConfigs) ->
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<WireTestServerReceiver>()

            clientConfigs.forEach { config ->
                val serverReceiver = WireTestServerReceiver { _, _ -> }
                serverReceiverList.add(serverReceiver)
                server?.registerChannelMessageReceiver(config.port, serverReceiver)
                (1..config.totalClients).forEach { _ ->
                    clientFactory?.createClient("localhost", config.port)?.let { client ->
                        clientList.add(client)
                    }
                }
            }

            startServer(server!!)
            connectClients(clientList)
            clientList.forEach { client ->
                (0..msgCount).forEach { i ->
                    totalMessagesSent++
                    client.sendMessage(
                        KonemMessage(
                            messageType = MessageType.DATA,
                            data_ = Data("$i")
                        )
                    )
                }
            }

            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)

            serverReceiverList.forEach { receiver ->
                receiver.messageListByConnection.values.forEach { connectionMessages ->
                    val msgList = connectionMessages.toTypedArray<KonemMessage>()
                    msgList.size shouldBe msgCount + 1
                    (0..msgCount).forEach { i ->
                        val data = msgList[i].data_ as Data
                        data.data_.toInt() shouldBe i
                    }
                }
            }
        }
    }

    context(":Messages sent by server are received by clients in expected order") {
        withTests(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}" },
            ts = listOf(
                ClientCommConfigsV1(1500, mutableListOf(ClientConfig(6060, 1))),
                ClientCommConfigsV1(1500, mutableListOf(ClientConfig(6060, 5))),
                ClientCommConfigsV1(1500, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 1))),
                ClientCommConfigsV1(1500, mutableListOf(ClientConfig(6060, 10), ClientConfig(6061, 10))),
            ),
        ) { (msgCount, clientConfigs) ->
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val clientReceiverList = mutableListOf<WireTestClientReceiver>()

            clientConfigs.forEach { config ->
                (1..config.totalClients).forEach { i ->
                    clientFactory?.createClient("localhost", config.port)?.let { client ->
                        val clientReceiver = WireTestClientReceiver(client) { _, _ -> }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        client.registerChannelMessageReceiver(clientReceiver)
                        clientReceiverList.add(clientReceiver)
                        clientList.add(client)
                    }
                }
            }

            startServer(server!!)
            connectClients(clientList)

            clientConfigs.forEach { config ->
                (0..msgCount).forEach { i ->
                    server?.broadcastOnChannel(
                        config.port,
                        KonemMessage(
                            messageType = MessageType.DATA,
                            data_ = Data("$i")
                        )
                    )
                    totalMessagesSent += config.totalClients
                }
            }

            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)

            clientReceiverList.forEach { receiver ->
                receiver.messageListByConnection.values.forEach { connectionMessages ->
                    val msgList = connectionMessages.toTypedArray<KonemMessage>()
                    msgList.size shouldBe msgCount + 1
                    (0..msgCount).forEach { i ->
                        val data = msgList[i].data_ as Data
                        data.data_.toInt() shouldBe i
                    }
                }
            }
        }
    }
})
