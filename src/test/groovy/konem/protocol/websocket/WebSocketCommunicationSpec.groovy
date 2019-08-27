package konem.protocol.websocket

import groovyx.gpars.GParsPool
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.netty.stream.ConnectionListener
import konem.netty.stream.ConnectionStatusListener
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
        Thread.sleep(100)
    }


    def "Server receiver gets all messages when registered"() {
        given:
        def receiver
        receiver = new GroovyKonemMessageReceiver({ addr, msg ->
            receiver.messageCount++
        })

        server.registerChannelReadListener(receiver)
        server.startServer()
        TestUtil.waitForServerActive(server)

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

        TestUtil.waitForAllMessages(receiver, totalMessages, recieveTime)

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
        TestUtil.waitForServerActive(server)

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
        TestUtil.waitForAllMessages(receiverList, totalMessages * 2, recieveTime)

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
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 5_000    | 1000      | 5000
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
        TestUtil.waitForServerActive(server)

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
        TestUtil.waitForAllMessages(receiverSList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(receiverCList, totalMessages, recieveTime)
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
        TestUtil.waitForAllMessages(receiverSList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(receiverCList, totalMessages, recieveTime)


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

    def "Server's broadcastOnChannel sends to all clients on a port"() {
        given:
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def receiverCList = []

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
        configurations.each { config ->
            config.paths.each { path ->
                1.upto(messages) {
                    server.broadcastOnChannel(config.port, msg, path)
                }
            }

        }
        TestUtil.waitForAllMessages(receiverCList, totalMessages, recieveTime)

        then:
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

    def "Server's broadcastOnAllChannels sends to all clients on a port"() {
        given:
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def receiverCList = []

        def totalMessages = 0
        def totalClients = 0
        configurations.each { config ->
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
                        clientReceiver.messageCount++
                    })
                    totalClients++
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
        configurations.each { config ->
            config.paths.each { path ->
                1.upto(messages) {
                    server.broadcastOnAllChannels(msg)
                }
                totalMessages += (totalClients * messages)
            }
        }

        TestUtil.waitForAllMessages(receiverCList, totalMessages, recieveTime)

        then:
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
        configurations                                                    | messages | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 1_000    | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 1],
         [port: 7081, paths: ["/test2"], clients: 1]]                     | 606      | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 5],
         [port: 7081, paths: ["/test2"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5]] | 501      | 1000      | 5000
        [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
         [port: 7081, paths: ["/test2", "/test3"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5],
         [port: 7083, paths: ["/test7", "/test8", "/test9"], clients: 5]] | 505      | 1000      | 5000
    }


    def "Server can receive and then respond to correct clients"() {
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
        TestUtil.waitForServerActive(server)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += (config.clients * (messages) * config.paths.size)
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver("client-$it-$path", { addr, msg ->
                        clientReceiver.messageCount++
                        clientReceiver.messageList << msg
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

        clientList.each { WebSocketClient client ->
            client.getReadListeners().each { receiver ->
                1.upto(messages) {
                    client.sendMessage(new KonemMessage(new Message.Data(receiver.clientId)))
                }
            }
        }

        print "Server "
        TestUtil.waitForAllMessages(receiverSList, clientList.size() * messages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(receiverCList, clientList.size() * messages, recieveTime)

        then:
        clientList.each { WebSocketClient client ->
            def receivers = client.getReadListeners()
            receivers.each { receiver ->
                receiver.messageList.each { KonemMessage msg ->
                    assert msg.getKonemMessage().data == receiver.clientId
                }
            }
        }

        where:
        configurations                                                    | messages | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                     | 10       | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 1],
         [port: 7081, paths: ["/test2"], clients: 1]]                     | 50       | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 5],
         [port: 7081, paths: ["/test2"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5]] | 100      | 1000      | 5000
        [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
         [port: 7081, paths: ["/test2", "/test3"], clients: 5],
         [port: 7082, paths: ["/test4", "/test5", "/test6"], clients: 5],
         [port: 7083, paths: ["/test7", "/test8", "/test9"], clients: 5]] | 100      | 1000      | 5000
    }


    def "Reader can register for specific ws path and only get reads from that path"() {
        given:
        def receiverSList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })


        receiverPaths.each {
            if (it.isEmpty()) {
                server.registerChannelReadListener(serverReceiver)
            } else {
                server.registerChannelReadListener(serverReceiver, it)
            }
        }

        receiverSList << serverReceiver
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    client.connect()
                    clientList << client
                }

                def empty = false
                receiverPaths.each {
                    empty = false
                    if (it.isEmpty()) {
                        empty = true
                    }
                }
                if (receiverPaths.contains(path) || empty) {
                    totalMessages += (config.clients * (messages))
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

        TestUtil.waitForAllMessages(serverReceiver, totalMessages, recieveTime)
        then:
        println "${serverReceiver.messageCount} == ${totalMessages}"

        assert serverReceiver.messageCount == totalMessages
        println "-----------------------------"
        where:
        receiverPaths        | configurations                                           | messages | sleepTime | recieveTime
        ["/test0"]           | [[port: 7060, paths: ["/test0"], clients: 1]]            | 5        | 1000      | 1000
        [""]                 | [[port: 7060, paths: ["/test0"], clients: 1]]            | 5        | 1000      | 5000
        [""]                 | [[port: 7060, paths: ["/test0", "/test1"], clients: 1]]  | 5        | 1000      | 5000
        [""]                 | [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
                                [port: 7081, paths: ["/test2", "/test3"], clients: 5]]  | 5        | 1000      | 5000
        ["/test0"]           | [[port: 7060, paths: ["/test0", "/test1"], clients: 1]]  | 5        | 1000      | 5000
        ["/test0", "/test3"] | [[port: 7060, paths: ["/test0", "/test1"], clients: 5]]  | 5        | 1000      | 5000
        ["/test0", "/test3"] | [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
                                [port: 7081, paths: ["/test2", "/test3"], clients: 5]]  | 5        | 1000      | 5000
        ["/test0", "/test1",
         "/test2", "/test3",
         "/test4"]           | [[port: 7060, paths: ["/test0", "/test1"], clients: 5],
                                [port: 7081, paths: ["/test2", "/test3"], clients: 5]]  | 32       | 1000      | 5000
        ["/test0", "/test1",
         "/test2", "/test3",
         "/test4"]           | [[port: 7060, paths: ["/test0", "/test1"], clients: 11],
                                [port: 7081, paths: ["/test2", "/test3"], clients: 27]] | 17       | 1000      | 5000
    }


    def "Clients ConnectionListener is called after connect and able to send message"() {
        given:
        def receiverSList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.registerChannelReadListener(serverReceiver)

        def connectionList = []
        server.registerConnectionListener(new ConnectionListener({ addr ->
            connectionList << addr
        }))

        receiverSList << serverReceiver
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            config.paths.each { path ->
                totalMessages += config.clients
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    client.registerConnectionListener(new ConnectionListener({ addr ->
                        client.sendMessage(new KonemMessage(new Message.Data("Send")))
                    }))
                    clientList << client
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:

        TestUtil.waitForAllMessages(receiverSList, totalMessages, recieveTime)

        then:

        def serverMessages = serverReceiver.messageCount
        println "server recieved $serverMessages == $totalMessages client messages sent"
        serverMessages == totalMessages
        println "server connections ${connectionList.size()} == $totalMessages clients "
        def serverSize = connectionList.size()
        serverSize == totalMessages
        println "-----------------------------"
        where:
        configurations                                                     | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                      | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50]]                     | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50]]           | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50],
         [port: 7082, paths: ["/test5", "/test4", "/test6"], clients: 75]] | 1000      | 5000

    }

    def "Server's ConnectionListener is called after a client connects and able to send message to client"() {
        given:
        def receiverSList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.registerChannelReadListener(serverReceiver)

        def connectionList = []
        server.registerConnectionListener(new ConnectionListener({ addr ->
            connectionList << addr
            server.sendMessage(addr, new KonemMessage(new Message.Data("Send")))
        }))

        receiverSList << serverReceiver
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def receiverCList = []
        def totalMessages = 0
        configurations.each { config ->
            config.paths.each { path ->
                totalMessages += config.clients
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    clientList << client
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
                        clientReceiver.messageCount++
                    })
                    client.registerChannelReadListener(clientReceiver)
                    receiverCList << clientReceiver
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:

        TestUtil.waitForAllMessages(receiverCList, totalMessages, recieveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { WebSocketClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "Server sent $totalMessages == $clientMessagesRecieved client messages got"
        assert totalMessages == clientMessagesRecieved
        println "server connections ${connectionList.size()} == $totalMessages clients "
        def serverSize = connectionList.size()
        assert serverSize == totalMessages
        println "-----------------------------"
        where:
        configurations                                                     | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                      | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50]]                     | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50]]           | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50],
         [port: 7082, paths: ["/test5", "/test4", "/test6"], clients: 75]] | 1000      | 5000
    }

    def "ConnectionStatusListener's connect and disconnect listeners are called"() {
        given:
        def receiverSList = []
        def serverReceiver
        serverReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.registerChannelReadListener(serverReceiver)

        def connections = 0
        def disconnections = 0
        server.registerConnectionStatusListener(new ConnectionStatusListener(
                { addr ->
                    connections++
                    server.sendMessage(addr, new KonemMessage(new Message.Data("Send")))
        },
                {addr ->
                    disconnections++
        }))

        receiverSList << serverReceiver
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def receiverCList = []
        def totalMessages = 0
        configurations.each { config ->
            config.paths.each { path ->
                totalMessages += config.clients
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    clientList << client
                    def clientReceiver
                    clientReceiver = new GroovyKonemMessageReceiver({ addr, msg ->
                        clientReceiver.messageCount++
                        client.disconnect()
                    })
                    client.registerChannelReadListener(clientReceiver)
                    receiverCList << clientReceiver
                    client.connect()
                }
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        TestUtil.ensureDisconnected(clientList)
        Thread.sleep(sleepTime*2)
        then:
        println "ConnectionStatusListener saw connections: $connections  == ${clientList.size()} clients "
        assert connections == clientList.size()
        println "ConnectionStatusListener saw disconnections: $disconnections  == ${clientList.size()} clients "
        assert disconnections == clientList.size()
        println "-----------------------------"
        where:
        configurations                                                     | sleepTime | recieveTime
        [[port: 7060, paths: ["/test0"], clients: 1]]                      | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50]]                     | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50]]           | 1000      | 5000
        [[port: 7060, paths: ["/test0"], clients: 50],
         [port: 7081, paths: ["/test2", "/test3"], clients: 50],
         [port: 7082, paths: ["/test5", "/test4", "/test6"], clients: 75]] | 1000      | 5000
    }

}