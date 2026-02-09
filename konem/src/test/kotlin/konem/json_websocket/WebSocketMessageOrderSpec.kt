package konem.json_websocket

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.concurrency.TestExecutionMode
import io.kotest.matchers.shouldBe
import konem.DEBUG
import konem.Konem
import konem.WsClientCommConfigsV1
import konem.WsClientConfig
import konem.connectClients
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.client.Client
import konem.protocol.konem.KonemProtocolPipeline
import konem.startServer
import konem.waitForMessagesClient
import konem.waitForMessagesServer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class WebSocketMessageOrderSpec : FunSpec({
    testExecutionMode = TestExecutionMode.Sequential

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
        delay(500.milliseconds)
    }

    beforeTest {
        server = Konem.createWebSocketServer(
            config = {
                addChannel(6060, "/test0", "/test1")
                addChannel(6061, "/test2", "/test3")
                addChannel(6062, "/test4", "/test5", "/test6")
                addChannel(6063, "/test7", "/test8", "/test9", "/test10")
            },
            protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
        )
        clientFactory = Konem.createWebSocketClientFactoryOfDefaults(
            KonemProtocolPipeline.getKonemJsonPipeline()
        )
    }

    context(":Messages sent by client are received in expected order") {
        withTests(
            nameFn = { data: WsClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}" },
            ts = listOf(
                WsClientCommConfigsV1(
                    1500, mutableListOf(
                        WsClientConfig(6060, 1, listOf("/test0"))
                    )
                ),
                WsClientCommConfigsV1(
                    1000, mutableListOf(
                        WsClientConfig(6060, 5, listOf("/test0"))
                    )
                ),
                WsClientCommConfigsV1(1000,
                    mutableListOf(
                        WsClientConfig(6060,1,listOf("/test0")),
                        WsClientConfig(6061,1,listOf("/test2"))
                    )
                ),
                WsClientCommConfigsV1(1000,
                    mutableListOf(
                        WsClientConfig(6060,5,listOf("/test0")),
                        WsClientConfig(6061,5,listOf("/test2"))
                    )
                ),
                WsClientCommConfigsV1(1000,
                    mutableListOf(
                        WsClientConfig(6060,1,listOf("/test0")),
                        WsClientConfig(6061,1,listOf("/test2")),
                        WsClientConfig(6062,1,listOf("/test4")),
                        WsClientConfig(6063,1,listOf("/test10"))
                    )
                ),
                WsClientCommConfigsV1(750,
                mutableListOf(
                    WsClientConfig(6060,5,listOf("/test0")),
                    WsClientConfig(6061,5,listOf("/test2")),
                    WsClientConfig(6062,5,listOf("/test4")),
                    WsClientConfig(6063,5,listOf("/test10"))
                    )
                )
            ),
        ) { (msgCount, clientConfigs) ->
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestWebSocketServerReceiver>()


            clientConfigs.forEach { config ->

                config.paths.forEach { path ->
                    var serverReceiver = JsonTestWebSocketServerReceiver() { _, _ -> }
                    serverReceiverList.add(serverReceiver)
                    server?.registerChannelMessageReceiver(
                        config.port,
                        serverReceiver,
                        path
                    )
                    (1..config.totalClients).forEach { _ ->
                        clientFactory?.createClient("localhost", config.port, path)?.let {
                            clientList.add(it)
                        }
                    }
                }

            }

            startServer(server!!)
            connectClients(clientList)
            totalMessagesSent += sendClientMessagesCounted(msgCount, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            serverReceiverList.forEach { receiver ->
                receiver.messageListByConnection.values.forEach { connectionMessages ->
                    val msgList = connectionMessages.toTypedArray<KonemMessage>()
                    msgList.size shouldBe msgCount + 1
                    (0..msgCount).forEach { i ->
                        val data: Data = msgList[i].message as Data
                        data.data.toInt() shouldBe i
                    }
                }
            }

            if (DEBUG) println("-----------------------------------")

        }
    }

    context(":Messages sent by server are received by clients in expected order") {
        withTests(
            nameFn = { data: WsClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}" },
            ts = listOf(
                WsClientCommConfigsV1(
                    1500, mutableListOf(
                        WsClientConfig(6060, 1, listOf("/test0"))
                    )
                ),
                WsClientCommConfigsV1(
                    1500, mutableListOf(
                        WsClientConfig(6060, 5, listOf("/test0"))
                    )
                ),
                WsClientCommConfigsV1(
                    1500, mutableListOf(
                        WsClientConfig(6060, 1, listOf("/test0")),
                        WsClientConfig(6061, 1, listOf("/test2"))
                    )
                ),
                WsClientCommConfigsV1(
                    1500, mutableListOf(
                        WsClientConfig(6060, 10, listOf("/test0")),
                        WsClientConfig(6061, 10, listOf("/test2"))
                    )
                )
            ),
        ) { (msgCount, clientConfigs) ->
            var totalMessagesSent = 0
            val totalConnections = countExpectedWebSocketConnections(clientConfigs)
            val clientList = mutableListOf<Client<KonemMessage>>()
            val clientReceiverList = mutableListOf<JsonTestWebSocketClientReceiver>()

            clientConfigs.forEach { config ->
                config.paths.forEach { path ->
                    (1..config.totalClients).forEach { i ->
                        clientFactory?.createClient("localhost", config.port, path)?.let { client ->
                            val clientReceiver = JsonTestWebSocketClientReceiver(client) { _, _ ->
                            }

                            clientReceiver.clientId = "client-$i-${config.port}-$path"
                            client.registerChannelMessageReceiver(clientReceiver)
                            clientReceiverList.add(clientReceiver)
                            clientList.add(client)
                        }
                    }
                }
            }

            startServer(server!!)
            connectClientsAndWaitForServerConnections(clientList, totalConnections, DEBUG)

            clientConfigs.forEach { config ->
                config.paths.forEach { path ->
                    (0..msgCount).forEach { i ->
                        server?.broadcastOnChannel(config.port, KonemMessage(message = Data("$i")), path)
                        totalMessagesSent += config.totalClients
                    }
                }
            }

            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)

            clientReceiverList.forEach { receiver ->
                receiver.messageListByConnection.values.forEach { connectionMessages ->
                    val msgList = connectionMessages.toTypedArray<KonemMessage>()
                    msgList.size shouldBe msgCount + 1
                    (0..msgCount).forEach { i ->
                        val data: Data = msgList[i].message as Data
                        data.data.toInt() shouldBe i
                    }
                }
            }

            if (DEBUG) println("-----------------------------------")
        }
    }


})
