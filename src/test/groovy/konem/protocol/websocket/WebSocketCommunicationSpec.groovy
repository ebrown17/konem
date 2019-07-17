package konem.protocol.websocket

import groovyx.gpars.GParsPool
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.testUtil.GroovyKonemMessageReceiver
import konem.testUtil.TestUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification


/**
 *  + server receivers can register before server starts and then see messages
 *  + server receivers can register after server starts and then see messages
 *  + clients can register receiver before connect and then see messages
 *  + clients can register after connect and still see messages
 *  + client receivers can see messages after a reconnect
 *  + server can broadcast to all clients on a port
 *  + server can broad cast to all clients on all ports
 *  + server can send to specific clients
 *  + server can responds to correct client
 *  + server receives all messages a client sends
 *  + client can send and receive both json and plain text
 *  + server can send and receive both json and plain text
 *  + client receives ping messages ( verified by reading io.netty.handler.codec.http.websocketx.WebSocketProtocolHandlder's decode method)
 *  + clients receive correct messages back from server
 *  + receiver can register for specific ws path and only get reads from that
 *  + receiver with no path specified receives reads for all paths
 */

class WebSocketCommunicationSpec extends Specification {

    @Shared
    WebSocketServer server

    @Shared
    WebSocketClientFactory factory

    @Shared
    KonemMessageSerializer serializer = TestUtil.serializer

    @Shared
    Logger logger = LoggerFactory.getLogger("WebSocketCommunicationSpec")

    def setup() {
        factory = new WebSocketClientFactory()
        server = new WebSocketServer()
        server.addChannel(7060, "/test0", "/test1")
        server.addChannel(7081, "/test2", "/test3")
        server.addChannel(7082, "/test4", "/test5", "/test6")
        server.addChannel(7083, "/test7", "/test8", "/test9")

    }

    def cleanup() {
        server.shutdownServer()
        server = null
        factory.shutdown()
        factory = null
        Thread.sleep(2000)
    }


    def "Server receiver gets all messages when registered"() {
        given:
        def receiver
        receiver = new GroovyKonemMessageReceiver({ addr, msg ->
            receiver.messageCount++
        })

        server.registerChannelReadListener(receiver)
        server.startServer()
        Thread.sleep(sleepTime)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += (config.clients * (messages) * config.paths.size)
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    clientList << client
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        def msg = new KonemMessage(new Message.Data("send"))
        GParsPool.withPool(clientList.size()) {
            clientList.eachParallel { WebSocketClient client ->
                1.upto(messages) {
                    client.sendMessage(msg)
                }
            }
        }

        TestUtil.waitForAllMessges(receiver, totalMessages, recieveTime)

        then:
        println "$configurations messages: $messages"
        println "${receiver.messageCount} == $totalMessages"
        assert receiver.messageCount == totalMessages
        println "--------------------------------"

        where:
        configurations                                                    | messages | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 5_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 1],
         [port: 7081, paths: ["/test2"], clients: 1]]                     | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 5],
         [port: 7081, paths: ["/test2"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5]] | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
         [port: 7081, paths: ["/test2", "/test3"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5],
         [port: 7083, paths: ["/test7", "/test8", "/test9"], clients: 5]] | 1_000    | 1000      | 5000
    }


    def "Clients can register receiver before connect and see messages"() {
        given:
        def receiverList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr, msg)
        })
        receiverList << serverReceiver
        server.registerChannelReadListener(serverReceiver)
        server.startServer()
        Thread.sleep(sleepTime)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += (config.clients * (messages) * config.paths.size)
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
                        clientReceiver.messageCount++
                    })
                    clientList << client
                    receiverList << clientReceiver
                    client.registerChannelReadListener(clientReceiver)
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        def msg = new KonemMessage(new Message.Data("send"))
        GParsPool.withPool(clientList.size()) {
            clientList.eachParallel { WebSocketClient client ->
                1.upto(messages) {
                    client.sendMessage(msg)
                }
            }
        }
        TestUtil.waitForAllMessges(receiverList, totalMessages * 2, recieveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { WebSocketClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved $totalMessages == $clientMessagesRecieved client messages"
        totalMessages == clientMessagesRecieved
        println "-----------------------------"
        where:
        configurations                                                    | messages | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 5_000   | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 1],
         [port: 7081, paths: ["/test2"], clients: 1]]                     | 5_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 5],
         [port: 7081, paths: ["/test2"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5]] | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
         [port: 7081, paths: ["/test2", "/test3"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5],
         [port: 7083, paths: ["/test7", "/test8", "/test9"], clients: 5]] | 1_000    | 1000      | 5000
    }


    def "Clients can see messages after a reconnect"() {
        given:
        def receiverSList = []
        def receiverCList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr as InetSocketAddress, msg as KonemMessage)
        })
        receiverSList << serverReceiver
        server.registerChannelReadListener(serverReceiver)
        server.startServer()
        Thread.sleep(sleepTime)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += (config.clients * (messages) * config.paths.size)
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
                        clientReceiver.messageCount++
                    })
                    clientList << client
                    receiverCList << clientReceiver
                    client.registerChannelReadListener(clientReceiver)
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        def msg = new KonemMessage(new Message.Data("send"))
        GParsPool.withPool(clientList.size()) {
            clientList.eachParallel { WebSocketClient client ->
                1.upto(messages) {
                    client.sendMessage(msg)
                }
            }
        }
        print "Server "
        TestUtil.waitForAllMessges(receiverSList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessges(receiverCList, totalMessages, recieveTime)
        clientList.each { WebSocketClient client ->
            client.disconnect()
        }
        TestUtil.ensureDisconnected(clientList)

        clientList.each { WebSocketClient client ->
            client.connect()
        }

        TestUtil.ensureClientsActive(clientList)

        GParsPool.withPool(clientList.size()) {
            clientList.eachParallel { WebSocketClient client ->
                1.upto(messages) {
                    client.sendMessage(msg)
                }
            }
        }
        totalMessages += totalMessages
        print "Server "
        TestUtil.waitForAllMessges(receiverSList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessges(receiverCList, totalMessages, recieveTime)


        then:

        def clientMessagesRecieved = 0
        clientList.each { WebSocketClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${totalMessages} == $clientMessagesRecieved client messages"
        totalMessages == clientMessagesRecieved


        println "-----------------------------"
        where:
        configurations                                                    | messages | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 1],
         [port: 7081, paths: ["/test2"], clients: 1]]                     | 5_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 5],
         [port: 7081, paths: ["/test2"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5]] | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
         [port: 7081, paths: ["/test2", "/test3"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5],
         [port: 7083, paths: ["/test7", "/test8", "/test9"], clients: 5]] | 1_000    | 1000      | 5000
    }
    /*
     def "Server can broadcast to all clients on a port"() {
         given:
         server.startServer()
         Thread.sleep(sleepTime)

         def clientList = []
         def receiverList = []

         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(config.clients) {
                     def client = factory.createClient("localhost", config.port, path)
                     ReadListener clientReader = new ReadListenerWebSocket() {
                         int messageCount = 0

                         @Override
                         synchronized void read(InetSocketAddress addr, String message) {
                             messageCount++
                         }

                         @Override
                         synchronized void read(InetSocketAddress addr, JsonNode message) {
                             messageCount++
                         }
                     }
                     client.connect()
                     clientList << client
                     receiverList << clientReader
                     client.registerChannelReadListener(clientReader)


                 }
             }
         }
         TestUtil.ensureClientsActive(clientList)

         when:
         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(stringMessages) {
                     server.broadcastOnChannel(config.port, "Server Message $it", path)

                 }
                 1.upto(jsonMessages) {
                     ObjectNode message = serializer.createObjectNode()
                     message.put("server_message$it", "$it")
                     server.broadcastOnChannel(config.port, message, path)
                 }


             }

         }
         TestUtil.waitForAllMessges(receiverList, recieveTime)

         then:
         def totalMessages = 0
         configurations.each { config ->
             config.paths.each { path ->
                 totalMessages += (config.clients * (stringMessages + jsonMessages))
             }
         }

         def clientMessagesRecieved = 0
         clientList.each { WebSocketClient client ->
             def receivers = client.getReadListeners()
             receivers.each {
                 clientMessagesRecieved += it.messageCount
             }
         }

         println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"
         totalMessages == clientMessagesRecieved
         println "-----------------------------"
         where:
         configurations                                                     | stringMessages | jsonMessages | sleepTime | recieveTime
         [[port: 6060, paths: ["/test0"], clients: 5]]                      | 5              | 5            | 500       | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 5]]            | 5              | 5            | 500       | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25]]           | 3              | 9            | 500       | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25]]           | 5              | 9            | 500       | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25]] | 15             | 9            | 500       | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25],
          [port: 6083, paths: ["/test7", "/test8", "/test9"], clients: 25]] | 20             | 21           | 500       | 500
     }

     def "Server can broadcast to all clients on all ports"() {
         given:
         server.startServer()
         Thread.sleep(sleepTime)

         def clientList = []
         def receiverList = []
         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(config.clients) {
                     def client = factory.createClient("localhost", config.port, path)
                     ReadListener clientReader = new ReadListenerWebSocket() {
                         int messageCount = 0

                         @Override
                         synchronized void read(InetSocketAddress addr, String message) {
                             messageCount++
                         }

                         @Override
                         synchronized void read(InetSocketAddress addr, JsonNode message) {
                             messageCount++
                         }
                     }
                     client.connect()
                     clientList << client
                     receiverList << clientReader
                     client.registerChannelReadListener(clientReader)


                 }
             }
         }
         TestUtil.ensureClientsActive(clientList)

         when:
         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(stringMessages) {
                     server.broadcastOnAllChannels("Server Message $it")

                 }
                 1.upto(jsonMessages) {
                     ObjectNode message = serializer.createObjectNode()
                     message.put("server_message$it", "$it")
                     server.broadcastOnAllChannels(message)
                 }
             }
         }
         TestUtil.waitForAllMessges(receiverList, recieveTime)

         then:
         def totalMessages = 0

         def totalPaths = 0
         configurations.each { config ->
             totalPaths += config.paths.size()
         }
         configurations.each { config ->

             config.paths.each { path ->
                 totalMessages += ((totalPaths * (config.clients * stringMessages)) + (totalPaths * (config.clients * jsonMessages)))
             }

         }
         def clientMessagesRecieved = 0
         clientList.each { WebSocketClient client ->
             def receivers = client.getReadListeners()
             receivers.each {
                 clientMessagesRecieved += it.messageCount
             }
         }

         println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"
         totalMessages == clientMessagesRecieved
         println "-----------------------------"
         where:
         configurations                                                     | stringMessages | jsonMessages | sleepTime | recieveTime
         [[port: 6060, paths: ["/test0"], clients: 5]]                      | 5              | 5            | 2000      | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 5]]            | 11             | 15           | 2000      | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 5]]            | 5              | 5            | 2000      | 1500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25]]           | 3              | 9            | 2000      | 1500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25]]           | 5              | 9            | 2000      | 4500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25]] | 15             | 9            | 2000      | 4500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25],
          [port: 6083, paths: ["/test7", "/test8", "/test9"], clients: 25]] | 20             | 21           | 2000      | 4500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 11],
          [port: 6081, paths: ["/test2", "/test3"], clients: 31],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 7],
          [port: 6083, paths: ["/test7", "/test8", "/test9"], clients: 23]] | 20             | 21           | 2000      | 4500
     }

     def "Server can receive and then respond to correct clients"() {
         given:
         def receiverList = []
         ReadListener serverReader = new ReadListenerWebSocket() {
             int messageCount = 0

             @Override
             synchronized void read(InetSocketAddress addr, String message) {
                 messageCount++
                 server.sendMessage(addr, message)
             }

             @Override
             synchronized void read(InetSocketAddress addr, JsonNode message) {
                 messageCount++
                 server.sendMessage(addr, message)
             }
         }
         receiverList << serverReader
         server.registerChannelReadListener(serverReader)
         server.startServer()
         Thread.sleep(sleepTime)

         def clientList = []

         configurations.each { config ->
             config.paths.each { path ->

                 def clientNum = 1

                 1.upto(config.clients) {
                     def client = factory.createClient("localhost", config.port, path)
                     ReadListener clientReader = new ReadListenerWebSocket() {
                         String clientId = "client-$clientNum-$path"
                         def messageCount = 0

                         def messageList = []

                         @Override
                         synchronized void read(InetSocketAddress addr, String message) {
                             messageList << message
                             messageCount++
                         }

                         @Override
                         synchronized void read(InetSocketAddress addr, JsonNode message) {
                             messageList << message.get("clientId").textValue()
                             messageCount++
                         }
                     }

                     clientNum++
                     client.connect()
                     receiverList << clientReader
                     clientList << client
                     client.registerChannelReadListener(clientReader)


                 }
             }
         }
         TestUtil.ensureClientsActive(clientList)

         when:

         clientList.each { WebSocketClient client ->

             client.getReadListeners().each { receiver ->
                 1.upto(stringMessages) {
                     client.sendMessage(receiver.clientId)

                 }
                 1.upto(jsonMessages) {
                     ObjectNode message = serializer.createObjectNode()
                     message.put("clientId", "${receiver.clientId}")
                     client.sendMessage(message)

                 }
             }
         }

         TestUtil.waitForAllMessges(receiverList, recieveTime)

         then:
         clientList.each { WebSocketClient client ->
             def receivers = client.getReadListeners()
             receivers.each { receiver ->
                 receiver.messageList.each {
                     //println "$it == ${receiver.clientId}"
                     assert it == receiver.clientId
                 }
             }
         }

         where:
         configurations                                                     | stringMessages | jsonMessages | sleepTime | recieveTime
         [[port: 6060, paths: ["/test0"], clients: 1]]                      | 5              | 5            | 2000      | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 1]]            | 5              | 5            | 2000      | 500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25]]           | 3              | 9            | 2000      | 1500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25]]           | 5              | 9            | 2000      | 2500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25]] | 15             | 9            | 2000      | 2500
         [[port: 6060, paths: ["/test0", "/test1"], clients: 25],
          [port: 6081, paths: ["/test2", "/test3"], clients: 25],
          [port: 6082, paths: ["/test4", "/test5", "/test6"], clients: 25],
          [port: 6083, paths: ["/test7", "/test8", "/test9"], clients: 25]] | 20             | 21           | 2000      | 2500
     }

     def "Reader can register for specific ws path and only get reads from that path"() {
         given:
         def receiverList = []
         ReadListener serverReader = new ReadListenerWebSocket() {
             int messageCount = 0

             @Override
             synchronized void read(InetSocketAddress addr, String message) {
                 messageCount++
             }

             @Override
             synchronized void read(InetSocketAddress addr, JsonNode message) {
                 messageCount++
             }
         }

         receiverPaths.each {
             if (it.isEmpty()) {
                 server.registerChannelReadListener(serverReader)
             } else {
                 server.registerChannelReadListener(it, serverReader)
             }
         }

         receiverList << serverReader
         server.startServer()
         Thread.sleep(sleepTime)

         def clientList = []

         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(config.clients) {
                     def client = factory.createClient("localhost", config.port, path)
                     client.connect()
                     clientList << client
                 }
             }
         }
         TestUtil.ensureClientsActive(clientList)

         when:
         clientList.each { WebSocketClient client ->
             1.upto(stringMessages) {
                 client.sendMessage("SEND")
             }
             1.upto(jsonMessages) {
                 ObjectNode message = serializer.createObjectNode()
                 message.put("clientId", "SEND")
                 client.sendMessage(message)
             }
         }

         Thread.sleep(recieveTime)

         then:
         def totalMessages = 0
         configurations.each { config ->
             config.paths.each {
                 def empty = false
                 receiverPaths.each {
                     empty = false
                     if (it.isEmpty()) {
                         empty = true
                     }
                 }
                 if (receiverPaths.contains(it) || empty) {
                     totalMessages += (config.clients * (stringMessages + jsonMessages))
                 }
             }

         }

         println "${serverReader.messageCount} == ${totalMessages}"

         serverReader.messageCount == totalMessages
         println "-----------------------------"
         where:
         receiverPaths        | configurations                                           | stringMessages | jsonMessages | sleepTime | recieveTime
         ["/test0"]           | [[port: 6060, paths: ["/test0"], clients: 1]]            | 5              | 5            | 2000      | 500
         [""]                 | [[port: 6060, paths: ["/test0"], clients: 1]]            | 5              | 5            | 2000      | 500
         [""]                 | [[port: 6060, paths: ["/test0", "/test1"], clients: 1]]  | 5              | 5            | 2000      | 500
         [""]                 | [[port: 6060, paths: ["/test0", "/test1"], clients: 5],
                                 [port: 6081, paths: ["/test2", "/test3"], clients: 5]]  | 5              | 5            | 2000      | 500
         ["/test0"]           | [[port: 6060, paths: ["/test0", "/test1"], clients: 1]]  | 5              | 5            | 2000      | 500
         ["/test0", "/test3"] | [[port: 6060, paths: ["/test0", "/test1"], clients: 5]]  | 5              | 5            | 2000      | 500
         ["/test0", "/test3"] | [[port: 6060, paths: ["/test0", "/test1"], clients: 5],
                                 [port: 6081, paths: ["/test2", "/test3"], clients: 5]]  | 5              | 5            | 2000      | 500
         ["/test0", "/test1",
          "/test2", "/test3",
          "/test4"]           | [[port: 6060, paths: ["/test0", "/test1"], clients: 5],
                                 [port: 6081, paths: ["/test2", "/test3"], clients: 5]]  | 32             | 7            | 2000      | 2000
         ["/test0", "/test1",
          "/test2", "/test3",
          "/test4"]           | [[port: 6060, paths: ["/test0", "/test1"], clients: 11],
                                 [port: 6081, paths: ["/test2", "/test3"], clients: 27]] | 17             | 24           | 2000      | 2000
     }

     def "Clients OnConnect listener is called after connect and able to send message"() {
         given:
         def receiverList = []
         ReadListener serverReader = new ReadListenerWebSocket() {
             int messageCount = 0

             @Override
             synchronized void read(InetSocketAddress addr, String message) {
                 messageCount++
             }

             @Override
             synchronized void read(InetSocketAddress addr, JsonNode message) {
                 messageCount++
             }
         }
         OnConnectionListener serverListener = new OnConnectionListener() {
             def connectionList = []

             @Override
             void onConnection(InetSocketAddress address) {
                 connectionList << address
             }

             @Override
             void onDisconnection(InetSocketAddress address) {
                 connectionList.remove(address)
             }
         }
         server.registerChannelReadListener(serverReader)
         server.registerOnConnectionListener(serverListener)
         receiverList << serverReader
         server.startServer()
         Thread.sleep(sleepTime)

         def clientList = []

         configurations.each { config ->
             config.paths.each { path ->
                 1.upto(config.clients) {
                     def client = factory.createClient("localhost", config.port, path)
                     OnConnectionListener connectionListener = new WebSocketClientConnectionListener() {
                         def id = "Client $path $it"

                         @Override
                         void onConnect(InetSocketAddress address) {
                             client.sendMessage("Send $id")
                         }

                         @Override
                         void onDisconnect(InetSocketAddress address) {
                         }
                     }
                     clientList << client
                     client.registerOnConnectionListener(connectionListener)
                     client.connect()

                 }
             }
         }
         TestUtil.ensureClientsActive(clientList)

         when:

         TestUtil.waitForAllMessges(receiverList, recieveTime)

         then:
         def totalMessages = 0
         configurations.each { config ->
             config.paths.each { path ->
                 totalMessages += config.clients
             }
         }

         def serverMessages = serverReader.messageCount
         println "server recieved $serverMessages == $totalMessages client messages sent"
         serverMessages == totalMessages
         println "server connections ${serverListener.connectionList.size()} == $totalMessages clients "
         def serverSize = serverListener.connectionList.size()
         serverSize == totalMessages
         println "-----------------------------"
         where:
         configurations                                                     | sleepTime | recieveTime
         [[port: 6060, paths: ["/test0"], clients: 1]]                      | 2000      | 3000
         [[port: 6060, paths: ["/test0"], clients: 50]]                     | 2000      | 3000
         [[port: 6060, paths: ["/test0"], clients: 50],
          [port: 6081, paths: ["/test2", "/test3"], clients: 50]]           | 2000      | 4000
         [[port: 6060, paths: ["/test0"], clients: 50],
          [port: 6081, paths: ["/test2", "/test3"], clients: 50],
          [port: 6082, paths: ["/test5", "/test4", "/test6"], clients: 75]] | 2000      | 4000

     }
 */
}