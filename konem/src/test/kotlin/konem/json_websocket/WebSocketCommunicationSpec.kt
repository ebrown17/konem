package konem.json_websocket

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.concurrency.TestExecutionMode
import konem.DEBUG
import konem.Konem
import konem.WsClientCommConfigsV1
import konem.WsClientConfig
import konem.connectClients
import konem.data.json.KonemMessage
import konem.netty.client.Client
import konem.protocol.konem.KonemProtocolPipeline
import konem.startServer
import konem.waitForMessagesServer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class WebSocketCommunicationSpec: FunSpec ({
    testExecutionMode = TestExecutionMode.Sequential

    afterTest{
        clientFactory?.shutdown()
        server?.shutdownServer()
        delay(500.milliseconds)
    }

    beforeTest {
        server = Konem.createWebSocketServer(
            config = {
                addChannel(6060,"/test0","/test1")
                addChannel(6061,"/test2","/test3")
                addChannel(6062,"/test4","/test5","/test6")
                addChannel(6063,"/test7","/test8","/test9","/test10")
            },
            protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
        )
        clientFactory = Konem.createWebSocketClientFactoryOfDefaults(
           KonemProtocolPipeline.getKonemJsonPipeline()
        )
    }

    context(": Server receiver's can register and see messages"){
        withTests(
            nameFn = { data: WsClientCommConfigsV1 -> "${this.testCase.name.name} ${data.msgCount} ${data.clientConfigs}"},
            ts = listOf(
                WsClientCommConfigsV1(1, mutableListOf(
                    WsClientConfig(6060,1,listOf("/test0")))
                ),
                WsClientCommConfigsV1(1, mutableListOf(
                    WsClientConfig(6060,5,listOf("/test0","/test1")))
                ),
                WsClientCommConfigsV1(10,
                    mutableListOf(
                        WsClientConfig(6060,5,listOf("/test0","/test1")),
                        WsClientConfig(6061,15,listOf("/test2","/test3"))
                    )
                ),
                WsClientCommConfigsV1(1,
                    mutableListOf(
                        WsClientConfig(6060,15 ,listOf("/test0","/test1")),
                        WsClientConfig(6061,15,listOf("/test2","/test3")),
                        WsClientConfig(6062,15 ,listOf("/test4","/test5","/test6")),
                        WsClientConfig(6063,15,listOf("/test7","/test8","/test9"))
                    )
                )
            ),
        ) {(msgCount, clientConfigs) ->
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestWebSocketServerReceiver>()

            var serverReceiver =  JsonTestWebSocketServerReceiver() { _, _ -> }

            serverReceiverList.add(serverReceiver)
            clientConfigs.forEach{ config ->
                server?.registerChannelMessageReceiver(config.port,serverReceiver,*config.paths.toTypedArray())
                for( i in 1..config.totalClients){
                    for(path in config.paths) {
                        clientFactory?.createClient("localhost", config.port,path)?.let{
                            var clientReceiver = JsonTestWebSocketClientReceiver(it){_,_ -> }
                            clientReceiver.clientId = "client-$i-${config.port}-$path"
                            it.registerChannelMessageReceiver(clientReceiver)
                            clientList.add(it)
                        }
                    }
                }
            }

            startServer(server!!)
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount,clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")

        }
    }

})
