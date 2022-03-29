package konem.json

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import konem.data.json.KonemMessage
import konem.netty.tcp.client.Client
import konem.protocol.konem.json.JsonClient
import konem.protocol.konem.json.JsonClientFactory
import konem.protocol.konem.json.JsonServer

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class JsonCommunicationSpec : ShouldSpec({

    beforeContainer {
        server = JsonServer.create { config ->
            config.addChannel(6060)
            config.addChannel(6061)
            config.addChannel(6062)
            config.addChannel(6063)
        }

        clientFactory = JsonClientFactory.createDefault()
    }

    afterContainer {
        clientFactory?.shutdown()
        server?.shutdownServer()
    }

    should(": Server readers can register and then see messages: ") {
        withData(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
            ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(49, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )),
            ClientCommConfigsV1(66, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )),

        ) { (msgCount, clientConfigs ) ->
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
            }
            serverReceiverList.add(serverReceiver)

            clientConfigs.forEach { config ->
                server?.registerChannelReceiveListener(config.port, serverReceiver)
                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it){ _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        it.registerChannelReceiveListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }


    should(": Clients can register reader; connect; send and receive messages from server: ") {
        withData(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
            ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(49, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )),
            ClientCommConfigsV1(66, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )),
            ) { (msgCount, clientConfigs ) ->
            lateinit var serverReceiver: JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
                server?.sendMessage(from, msg)
            }
            serverReceiverList.add(serverReceiver)

            clientConfigs.forEach { config ->
                server?.registerChannelReceiveListener(config.port, serverReceiver)

                for (i in 1..config.totalClients) {
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReceiveListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")
        }
    }


    should(": Clients can register reader; connect and then can send and receive messages from server after a reconnect: ") {
        withData(
            nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
            ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
            ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
            ClientCommConfigsV1(49, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21)
            )),
            ClientCommConfigsV1(66, mutableListOf(
                ClientConfig(6060, 1), ClientConfig(6061, 10),
                ClientConfig(6062, 21), ClientConfig(6063, 43)
            )),
        ) { (msgCount, clientConfigs ) ->

            lateinit var serverReceiver : JsonTestServerReceiver
            var totalMessagesSent = 0
            val clientList = mutableListOf<Client<KonemMessage>>()
            val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
            val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

            serverReceiver = JsonTestServerReceiver { from, msg ->
                serverReceiver.messageCount++
                server?.sendMessage(from,msg)
            }
            serverReceiverList.add(serverReceiver)

            clientConfigs.forEach { config  ->
                server?.registerChannelReceiveListener(config.port, serverReceiver)

                for(i in 1..config.totalClients){
                    clientFactory?.createClient("localhost",config.port)?.let {
                        lateinit var clientReceiver: JsonTestClientReceiver
                        clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                            clientReceiver.messageCount++
                            clientReceiver.messageList.add(msg)
                        }
                        clientReceiver.clientId = "client-$i-${config.port}"
                        clientReceiverList.add(clientReceiver)
                        it.registerChannelReceiveListener(clientReceiver)
                        clientList.add(it)
                    }
                }
            }

            startServer()
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList)
            waitForMessagesClient(totalMessagesSent, clientReceiverList)
            disconnectClients(clientList)
            connectClients(clientList)
            totalMessagesSent += sendClientMessages(msgCount, clientList)
            waitForMessagesServer(totalMessagesSent, serverReceiverList, DEBUG)
            waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
            if (DEBUG) println("-----------------------------------")

        }
    }


   should(": Server's broadcastOnChannel sends to all clients on correct port: ") {
       withData(
           nameFn = { data: ClientCommConfigsV2 -> "${this.testCase.displayName} ${data.msgCount} ${data.broadcastPorts} ${data.clientConfigs}" },
           ClientCommConfigsV2(1, mutableListOf(6060), mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV2(5,  mutableListOf(6061),  mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV2(5, mutableListOf(6060), mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV2(35, mutableListOf(6060,6061),mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV2(49,mutableListOf(6060,6061), mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV2(66, mutableListOf(6060,6061,6062,6063), mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (msgCount,broadcastPorts, clientConfigs ) ->

           lateinit var serverReceiver: JsonTestServerReceiver
           var totalMessagesSent = 0
           val clientList = mutableListOf<Client<KonemMessage>>()
           val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
           val clientReceiverList = mutableListOf<JsonTestClientReceiver>()
           serverReceiver = JsonTestServerReceiver { from, msg ->
               serverReceiver.messageCount++
           }
           serverReceiverList.add(serverReceiver)

           clientConfigs.forEach { config ->
               server?.registerChannelReceiveListener(config.port, serverReceiver)
               if (config.port in broadcastPorts) {
                   totalMessagesSent += (config.totalClients * (msgCount))
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
                       it.registerChannelReceiveListener(clientReceiver)
                       clientList.add(it)
                   }
               }
           }

           startServer()
           connectClients(clientList)
           serverBroadcastOnChannels(msgCount,broadcastPorts )
           waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
           if (DEBUG) println("-----------------------------------")
       }
   }

   should(": Server's broadcastOnAllChannels sends to all clients on all ports: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1,  mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5,  mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5,  mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (msgCount, clientConfigs ) ->

           lateinit var serverReceiver: JsonTestServerReceiver
           var totalMessagesSent = 0
           val clientList = mutableListOf<Client<KonemMessage>>()
           val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
           val clientReceiverList = mutableListOf<JsonTestClientReceiver>()
           serverReceiver = JsonTestServerReceiver { _, _ ->
               serverReceiver.messageCount++
           }
           serverReceiverList.add(serverReceiver)

           clientConfigs.forEach { config ->
               server?.registerChannelReceiveListener(config.port, serverReceiver)

               totalMessagesSent += (config.totalClients * (msgCount))

               for (i in 1..config.totalClients) {
                   clientFactory?.createClient("localhost",config.port)?.let {
                       lateinit var clientReceiver: JsonTestClientReceiver
                       clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                           clientReceiver.messageCount++
                           clientReceiver.messageList.add(msg)
                       }
                       clientReceiver.clientId = "client-$i-${config.port}"
                       clientReceiverList.add(clientReceiver)
                       it.registerChannelReceiveListener(clientReceiver)
                       clientList.add(it)
                   }
               }
           }

           startServer()
           connectClients(clientList)
           serverBroadcastOnAllChannels(msgCount)
           waitForMessagesClient(totalMessagesSent, clientReceiverList, DEBUG)
           if (DEBUG) println("-----------------------------------")
       }
   }

   should(": Server can receive and then respond to correct clients: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (msgCount, clientConfigs ) ->
           lateinit var serverReceiver : JsonTestServerReceiver
           var totalMessagesSent = 0
           val clientList = mutableListOf<Client<KonemMessage>>()
           val serverReceiverList = mutableListOf<JsonTestServerReceiver>()
           val clientReceiverList = mutableListOf<JsonTestClientReceiver>()

           serverReceiver = JsonTestServerReceiver { from, msg ->
               serverReceiver.messageCount++
               server?.sendMessage(from,msg)
           }
           serverReceiverList.add(serverReceiver)

           clientConfigs.forEach { config  ->
               server?.registerChannelReceiveListener(config.port, serverReceiver)

               for(i in 1..config.totalClients){
                   clientFactory?.createClient("localhost",config.port)?.let {
                       lateinit var clientReceiver: JsonTestClientReceiver
                       clientReceiver = JsonTestClientReceiver(it) { _, msg ->
                           clientReceiver.messageCount++
                           clientReceiver.messageList.add(msg)
                       }
                       clientReceiver.clientId = "client-$i-${config.port}"
                       clientReceiverList.add(clientReceiver)
                       it.registerChannelReceiveListener(clientReceiver)
                       clientList.add(it)
                   }
               }
           }

           startServer()
           connectClients(clientList)
           totalMessagesSent += sendClientMessageWithReceiver(msgCount, clientReceiverList)

           waitForMessagesServer(totalMessagesSent, serverReceiverList,DEBUG)
           waitForMessagesReceiverClient(totalMessagesSent, clientReceiverList,DEBUG)

           if (DEBUG) println("-----------------------------------")

       }
   }

   should(": Each Client's ConnectionListener is called after connected to a server: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (_, clientConfigs ) ->

           val clientList = mutableListOf<Client<KonemMessage>>()
           val clientDiscList = mutableListOf<TestConnectionListener>()
           var totalClientConnections = 0

           lateinit var clientConnnectListenerGlobal: TestConnectionListener
           clientConnnectListenerGlobal = TestConnectionListener {
               clientConnnectListenerGlobal.connections++
           }

           clientConfigs.forEach { config ->
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

   should(": Server's ConnectionListener is called after each client connects: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (_, clientConfigs ) ->

           val clientList = mutableListOf<Client<KonemMessage>>()
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

           clientConfigs.forEach { config ->
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

   should(": Client's DisconnectionListener is called after a disconnect: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (_, clientConfigs ) ->

           val clientList = mutableListOf<Client<KonemMessage>>()
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

           clientConfigs.forEach { config ->
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

   should(": Server's DisconnectionListener is called after each client disconnects: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (_, clientConfigs ) ->

           val clientList = mutableListOf<Client<KonemMessage>>()
           var totalServerDisconnections = 0
           lateinit var serverDisconnectionListener: TestDisconnectionListener

           server?.let{ srv ->
               serverDisconnectionListener = TestDisconnectionListener {
                   serverDisconnectionListener.disconnections++
               }
               srv.registerDisconnectionListener(serverDisconnectionListener)
           }

           clientConfigs.forEach { config ->
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

   should(": Server and Client's ConnectionStatusListener is called after each connect and disconnect: ") {
       withData(
           nameFn = { data: ClientCommConfigsV1 -> "${this.testCase.displayName} ${data.msgCount} ${data.clientConfigs}" },
           ClientCommConfigsV1(1, mutableListOf(ClientConfig(6060, 1))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 10))),
           ClientCommConfigsV1(5, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(35, mutableListOf(ClientConfig(6060, 1), ClientConfig(6061, 10))),
           ClientCommConfigsV1(49, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21)
           )),
           ClientCommConfigsV1(66, mutableListOf(
               ClientConfig(6060, 1), ClientConfig(6061, 10),
               ClientConfig(6062, 21), ClientConfig(6063, 43)
           )),
       ) { (_, clientConfigs ) ->

           val clientList = mutableListOf<Client<KonemMessage>>()
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

           clientConfigs.forEach { config ->
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
