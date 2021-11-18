package konem.json

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import konem.protocol.socket.json.JsonClient
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class JsonCommunicationSpec : ShouldSpec({

    afterTest {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    should(": Server readers can register before server starts and then see messages") {
        forAll(
            row(1, arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(ClientConfig(6060, 10))),
            row(5, arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(35, arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(
                49, arrayOf(
                    ClientConfig(6060, 1), ClientConfig(6061, 10),
                    ClientConfig(6062, 21)
                )
            ),
            row(
                66, arrayOf(
                    ClientConfig(6060, 1), ClientConfig(6061, 10),
                    ClientConfig(6062, 21), ClientConfig(6063, 43)
                )
            ),
        ) { sends, configs ->
            testSetup()
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config ->
                server?.registerChannelReadListener(config.port, serverReceiver)
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(sends, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Clients can register reader; connect and then can send and receive messages from server") {
        forAll(
            row(1, arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(ClientConfig(6060, 10))),
            row(5, arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(35, arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(
                49, arrayOf(
                    ClientConfig(6060, 1), ClientConfig(6061, 10),
                    ClientConfig(6062, 21)
                )
            ),
            row(
                66, arrayOf(
                    ClientConfig(6060, 1), ClientConfig(6061, 10),
                    ClientConfig(6062, 21), ClientConfig(6063, 43)
                )
            ),
        ) { sends, configs ->
            testSetup()
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
                server?.sendMessage(from, msg)
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config ->
                server?.registerChannelReadListener(config.port, serverReceiver)

                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(sends, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Clients can register reader; connect and then can send and receive messages from server after a reconnect") {
        forAll(
            row(1, arrayOf(ClientConfig(6060,1))),
            row(5, arrayOf(ClientConfig(6060,10))),
            row(5, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(35, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(49, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21))),
            row(66, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21), ClientConfig(6063,43))),
        ) { sends, configs ->
            testSetup()
            lateinit var serverReceiver : JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
                server?.sendMessage(from,msg)
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config  ->
                server?.registerChannelReadListener(config.port, serverReceiver)

                for(i in 1..config.totalClients){
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(sends, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList)
            waitForMessagesClient(totalMessagesSent, clientReceiverList)
            disconnectClients(clientList)
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(sends, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")

        }
    }

    should(": Server's broadcastOnChannel sends to all clients on correct port") {
        forAll(
            row(5, arrayOf(6060), arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(6061), arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(6060), arrayOf(ClientConfig(6060, 5))),
            row(5, arrayOf(6060,6061), arrayOf(ClientConfig(6060, 5),
                ClientConfig(6061, 11))),
            row(25, arrayOf(6060,6061), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11))),
            row(25, arrayOf(6060,6061,6062,6063), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11))),
            row(50, arrayOf(6060,6061,6062,6063), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11),
                ClientConfig(6063, 25))),
        ) { sends,broadcastPorts, configs ->
            testSetup()
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()
            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config ->
                server?.registerChannelReadListener(config.port, serverReceiver)
                if (config.port in broadcastPorts) {
                    totalMessagesSent += (config.totalClients * (sends))
                }
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            serverBroadcastOnChannels(sends,broadcastPorts )
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Server's broadcastOnAllChannels sends to all clients on all ports") {
        forAll(
            row(5, arrayOf(6060), arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(6061), arrayOf(ClientConfig(6060, 1))),
            row(5, arrayOf(6060), arrayOf(ClientConfig(6060, 5))),
            row(5, arrayOf(6060,6061), arrayOf(ClientConfig(6060, 5),
                ClientConfig(6061, 11))),
            row(25, arrayOf(6060,6061), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11))),
            row(25, arrayOf(6060,6061,6062,6063), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11))),
            row(50, arrayOf(6060,6061,6062,6063), arrayOf(ClientConfig(6060, 15),
                ClientConfig(6061, 11),ClientConfig(6062, 11),
                ClientConfig(6063, 25))),
        ) { sends,broadcastPorts, configs ->
            testSetup()
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()
            serverReceiver = JsonTestServerReceiver { _, _ ->
                serverReceiver.messageCount++
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config ->
                server?.registerChannelReadListener(config.port, serverReceiver)

                totalMessagesSent += (config.totalClients * (sends))

                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            serverBroadcastOnAllChannels(sends)
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Server can receive and then respond to correct clients") {
        forAll(
            row(1, arrayOf(ClientConfig(6060,1))),
            row(5, arrayOf(ClientConfig(6060,10))),
            row(5, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(35, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(49, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21))),
            row(66, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21), ClientConfig(6063,43))),
        ) { sends, configs ->
            testSetup()
            lateinit var serverReceiver : JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<JsonClient>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
                server?.sendMessage(from,msg)
            }
            serverReceiverList.add(serverReceiver)

            configs.forEach { config  ->
                server?.registerChannelReadListener(config.port, serverReceiver)

                for(i in 1..config.totalClients){
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReadListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessageWithReceiver(sends, clientReceiverList)

            waitForMessagesServer(totalMessagesSent, serverReceiverList,DEBUG)
            waitForMessagesReceiverClient(totalMessagesSent, clientReceiverList,DEBUG)

            if (DEBUG) println("-----------------------------------")

        }
    }

    should(": Each Client's ConnectionListener is called after connected to a server") {
        forAll(
            row(arrayOf(ClientConfig(6060, 1))),
            row(arrayOf(ClientConfig(6060, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )
            ),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )
            ),
        ) { configs ->
            testSetup()

            val clientList = mutableListOf<JsonClient>()
            val clientDiscList = mutableListOf<TestConnectionListener>()
            var totalClientConnections = 0

            lateinit var clientConnnectListenerGlobal: TestConnectionListener
            clientConnnectListenerGlobal = TestConnectionListener {
                clientConnnectListenerGlobal.connections++
            }

            configs.forEach { config ->
                totalClientConnections += config.totalClients
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let { client ->
                        lateinit var clientConnnectListener: TestConnectionListener
                        clientConnnectListener = TestConnectionListener {
                            clientConnnectListener.connections++
                        }
                        client.registerConnectionListener(clientConnnectListener)
                        client.registerConnectionListener(clientConnnectListenerGlobal)
                        clientDiscList.add(clientConnnectListener)
                        clientList.add(client)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))

            waitForClientStatusChange(totalClientConnections, mutableListOf(clientConnnectListenerGlobal), DEBUG)
            waitForClientStatusChange(totalClientConnections,clientDiscList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }

    }

    should(": Server's ConnectionListener is called after each client connects") {
        forAll(
            row(arrayOf(ClientConfig(6060, 1))),
            row(arrayOf(ClientConfig(6060, 10))),
            row(arrayOf(ClientConfig(6060, 11), ClientConfig(6061, 10))),
            row(arrayOf(ClientConfig(6060, 11), ClientConfig(6061, 20))),
            row(arrayOf(
                ClientConfig(6060, 11), ClientConfig(6061, 20),
                ClientConfig(6062, 21)
            )
            ),
            row(arrayOf(
                ClientConfig(6060, 11), ClientConfig(6061, 20),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )
            ),
        ) { configs ->
            testSetup()

            val clientList = mutableListOf<JsonClient>()
            val serverConList = mutableListOf<TestConnectionListener>()
            var totalServerConnections = 0
            lateinit var serverConnectionListener: TestConnectionListener

            server?.let{ srv ->
                serverConnectionListener = TestConnectionListener {
                    serverConnectionListener.connections++
                }
                srv.registerConnectionListener(serverConnectionListener)
                serverConList.add(serverConnectionListener)
            }

            configs.forEach { config ->
                totalServerConnections += config.totalClients
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let { client ->
                        clientList.add(client)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))

            waitForServerStatusChange(totalServerConnections,serverConList, DEBUG)

            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Client's DisconnectionListener is called after a disconnect") {
        forAll(
            row(arrayOf(ClientConfig(6060, 1))),
            row(arrayOf(ClientConfig(6060, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )
            ),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )
            ),
        ) { configs ->
            testSetup()

            val clientList = mutableListOf<JsonClient>()
            val serverConList = mutableListOf<TestConnectionListener>()
            val clientDiscList = mutableListOf<TestDisconnectionListener>()
            var totalServerConnections = 0
            var totalClientDisconnects = 0
            lateinit var serverConnectionListener: TestConnectionListener

            server?.let{ srv ->
                serverConnectionListener = TestConnectionListener {
                    serverConnectionListener.connections++
                }
                srv.registerConnectionListener(serverConnectionListener)
                serverConList.add(serverConnectionListener)
            }

            configs.forEach { config ->
                totalServerConnections += config.totalClients
                totalClientDisconnects = totalServerConnections
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let { client ->
                        lateinit var clientDisonnectListener: TestDisconnectionListener
                        clientDisonnectListener = TestDisconnectionListener {
                            clientDisonnectListener.disconnections++
                        }
                        client.registerDisconnectionListener(clientDisonnectListener)
                        clientDiscList.add(clientDisonnectListener)
                        clientList.add(client)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))

            waitForServerStatusChange(totalServerConnections,serverConList, DEBUG)
            server?.shutdownServer()
            delay(Duration.milliseconds(delayDurationMs))

            waitForClientStatusChange(totalClientDisconnects,clientDiscList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Server's DisconnectionListener is called after each client disconnects") {
        forAll(
            row(arrayOf(ClientConfig(6060, 1))),
            row(arrayOf(ClientConfig(6060, 10))),
            row(arrayOf(ClientConfig(6060, 11), ClientConfig(6061, 10))),
            row(arrayOf(ClientConfig(6060, 11), ClientConfig(6061, 20))),
            row(arrayOf(
                ClientConfig(6060, 11), ClientConfig(6061, 20),
                ClientConfig(6062, 21)
            )
            ),
            row(arrayOf(
                ClientConfig(6060, 11), ClientConfig(6061, 20),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )
            ),
        ) { configs ->
            testSetup()

            val clientList = mutableListOf<JsonClient>()
            var totalServerDisconnections = 0
            lateinit var serverDisconnectionListener: TestDisconnectionListener

            server?.let{ srv ->
                serverDisconnectionListener = TestDisconnectionListener {
                    serverDisconnectionListener.disconnections++
                }
                srv.registerDisconnectionListener(serverDisconnectionListener)
            }

            configs.forEach { config ->
                totalServerDisconnections += config.totalClients
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let { client ->
                        clientList.add(client)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            disconnectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))

            waitForServerStatusChange(totalServerDisconnections, mutableListOf(serverDisconnectionListener), DEBUG)

            if (DEBUG) println("-----------------------------------")
        }
    }

    should(": Server and Client's ConnectionStatusListener is called after each connect and disconnect") {
        forAll(
            row(arrayOf(ClientConfig(6060, 1))),
            row(arrayOf(ClientConfig(6060, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )
            ),
            row(arrayOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )
            ),
        ) { configs ->
            testSetup()

            val clientList = mutableListOf<JsonClient>()
            val clientStatusList = mutableListOf<TestConnectionStatusListener>()
            val serverStatusList = mutableListOf<TestConnectionStatusListener>()
            var totalConnections = 0
            lateinit var serverStatusListener: TestConnectionStatusListener

            server?.let{ srv ->
                serverStatusListener = TestConnectionStatusListener(
                    connected = { serverStatusListener.connections++ },
                    disconnected = { serverStatusListener.disconnections++ }
                )
                srv.registerConnectionStatusListener(serverStatusListener)
                serverStatusList.add(serverStatusListener)
            }

            lateinit var clientStatusListenerG: TestConnectionStatusListener
            clientStatusListenerG = TestConnectionStatusListener(
                connected = { clientStatusListenerG.connections++ },
                disconnected = { clientStatusListenerG.disconnections++ }
            )

            configs.forEach { config ->
                totalConnections += config.totalClients
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let { client ->
                        lateinit var clientStatusListener: TestConnectionStatusListener
                        clientStatusListener = TestConnectionStatusListener(
                            connected = { clientStatusListener.connections++ },
                            disconnected = { clientStatusListener.disconnections++ }
                        )
                        client.registerConnectionStatusListener(clientStatusListener)
                        client.registerConnectionStatusListener(clientStatusListenerG)
                        clientStatusList.add(clientStatusListener)
                        clientList.add(client)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))
            waitForServerStatusChange(totalConnections,serverStatusList, DEBUG,true)
            waitForClientStatusChange(totalConnections,clientStatusList, DEBUG,true)
            waitForClientStatusChange(totalConnections, mutableListOf(clientStatusListenerG), debug=false,true)
            disconnectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))
            waitForServerStatusChange(totalConnections,serverStatusList, DEBUG,false)
            waitForClientStatusChange(totalConnections,clientStatusList, DEBUG,false)
            waitForClientStatusChange(totalConnections, mutableListOf(clientStatusListenerG), debug=false,false)

            connectClients(clientList)
            delay(Duration.milliseconds(delayDurationMs))
            waitForServerStatusChange(totalConnections*2,serverStatusList, DEBUG,true)
            waitForClientStatusChange(totalConnections*2,clientStatusList, DEBUG,true)
            waitForClientStatusChange(totalConnections*2, mutableListOf(clientStatusListenerG), debug=false,true)

            server?.shutdownServer()
            delay(Duration.milliseconds(delayDurationMs))
            waitForClientStatusChange(totalConnections*2,clientStatusList, DEBUG,false)
            waitForClientStatusChange(totalConnections*2, mutableListOf(clientStatusListenerG), debug=false,false)

            if (DEBUG) println("-----------------------------------")
        }
    }
})
