package konem.protocol.socket.json

import konem.data.json.KonemMessage
import konem.netty.stream.ConnectionListener
import konem.netty.stream.ConnectionStatusListener
import konem.testUtil.GroovyJsonMessageReceiver
import konem.testUtil.TestUtil
import spock.lang.Shared
import spock.lang.Specification

class JsonCommunicationSpec extends Specification {
    @Shared
    JsonServer server

    @Shared
    JsonClientFactory factory

    def setup() {
        server = new JsonServer()
        server.addChannel(6060)
        server.addChannel(6081)
        server.addChannel(6082)
        server.addChannel(6083)

        factory = new JsonClientFactory()
    }

    def cleanup() {
        server.shutdownServer()
        factory.shutdown()
    }

    def "Server readers can register before server starts and then see messages"() {
        given:
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def totalMessages = 0

        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                clientList << client
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        TestUtil.waitForAllMessages(serverReceiver, totalMessages, receiveTime)

        then:

        serverReceiver.messageCount == totalMessages

        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 1        | 500
/*        [[port: 6060, clients: 10]] | 5        | 500
        [[port: 6060, clients: 1],
         [port: 6081, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14]] | 51       | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14],
         [port: 6082, clients: 21],
         [port: 6083, clients: 51]] | 21       | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14],
         [port: 6082, clients: 21],
         [port: 6083, clients: 51]] | 500      | 2000*/
    }

    def "Server readers can register after server starts and then see messages"() {
        given:
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.startServer()

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        TestUtil.waitForServerActive(server)

        def clientList = []
        def totalMessages = 0

        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                clientList << client
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        TestUtil.waitForAllMessages(serverReceiver, totalMessages, receiveTime)

        then:

        serverReceiver.messageCount == totalMessages

        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 10]] | 5        | 500
        [[port: 6060, clients: 1],
         [port: 6081, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14]] | 51       | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14],
         [port: 6082, clients: 21],
         [port: 6083, clients: 51]] | 21       | 500
        [[port: 6060, clients: 3],
         [port: 6081, clients: 14],
         [port: 6082, clients: 21],
         [port: 6083, clients: 51]] | 500      | 2000
    }

    def "Clients can register reader before connect and then see messages"() {
        given:
        def serverReceiverList = []
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << serverReceiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0

        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver

                client.registerChannelReadListener(clientReceiver)
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:

        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, receiveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)
        then:

        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${serverReceiver.messageCount} == $clientMessagesRecieved client messages"
        assert serverReceiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 25]] | 5        | 500
        [[port: 6060, clients: 25]] | 500      | 4000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17]] | 5        | 1000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17]] | 500      | 4000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17],
         [port: 6082, clients: 9]]  | 5        | 2000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17],
         [port: 6082, clients: 9],
         [port: 6083, clients: 56]] | 5        | 2000
    }

    def "Clients can register reader after connect and then see messages"() {
        given:
        def serverReceiverList = []
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << serverReceiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0

        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver

                client.connect()
                client.registerChannelReadListener(clientReceiver)
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:

        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, receiveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)
        then:

        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${serverReceiver.messageCount} == $clientMessagesRecieved client messages"
        assert serverReceiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 25]] | 5        | 500
        [[port: 6060, clients: 25]] | 500      | 4000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17]] | 5        | 1000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17]] | 500      | 4000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17],
         [port: 6082, clients: 9]]  | 5        | 2000
        [[port: 6060, clients: 31],
         [port: 6081, clients: 17],
         [port: 6082, clients: 9],
         [port: 6083, clients: 56]] | 5        | 2000
    }

    def "Client readers can see messages after a reconnect"() {
        given:
        def serverReceiverList = []
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << serverReceiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0

        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver

                client.connect()
                client.registerChannelReadListener(clientReceiver)
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:

        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, receiveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)

        clientList.each { JsonClient client ->
            client.disconnect()
        }
        TestUtil.ensureDisconnected(clientList)

        clientList.each { JsonClient client ->
            client.connect()
        }

        TestUtil.ensureClientsActive(clientList)

        clientList.eachWithIndex { JsonClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createKonemMessage(data))
            }
        }
        totalMessages += totalMessages
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, receiveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)

        then:

        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${serverReceiver.messageCount} == $clientMessagesRecieved client messages"
        assert serverReceiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 1]]  | 25       | 500
        [[port: 6060, clients: 33]] | 7        | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12]] | 25       | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29]] | 19       | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29]] | 75       | 2500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29],
         [port: 6083, clients: 7]]  | 75       | 2500

    }

    def "Server's broadcastOnChannel sends to all clients on a port"() {
        given:
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0

        configurations.each { config ->
            if (broadcastPorts.isEmpty()) {
                totalMessages += (config.clients * (messages))
            } else if (config.port in broadcastPorts) {
                totalMessages += (config.clients * (messages))
            }
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver

                client.connect()
                client.registerChannelReadListener(clientReceiver)
            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        if (broadcastPorts.isEmpty()) {
            1.upto(messages) {
                server.broadcastOnAllChannels(TestUtil.createKonemMessage("Server message $it"))
            }
        } else {
            broadcastPorts.each { port ->
                1.upto(messages) {
                    server.broadcastOnChannel(port, TestUtil.createKonemMessage("Server message $it"))
                }
            }
        }

        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"
        assert totalMessages == clientMessagesRecieved
        println "-----------------------------"
        where:
        broadcastPorts     | configurations              | messages | receiveTime
        [6060]             | [[port: 6060, clients: 1]]  | 5        | 500
        []                 | [[port: 6060, clients: 1]]  | 5        | 500
        [6060]             | [[port: 6060, clients: 5]]  | 5        | 500
        []                 | [[port: 6060, clients: 5]]  | 5        | 500
        [6060]             | [[port: 6060, clients: 5],
                              [port: 6081, clients: 5]]  | 5        | 500
        []                 | [[port: 6060, clients: 5],
                              [port: 6081, clients: 5]]  | 5        | 500
        [6060, 6081]       | [[port: 6060, clients: 15],
                              [port: 6081, clients: 5]]  | 11       | 500
        [6060]             | [[port: 6060, clients: 17],
                              [port: 6081, clients: 9],
                              [port: 6082, clients: 25]] | 11       | 500
        [6060, 6081]       | [[port: 6060, clients: 17],
                              [port: 6081, clients: 9],
                              [port: 6082, clients: 25]] | 11       | 500
        [6060, 6081, 6082] | [[port: 6060, clients: 17],
                              [port: 6081, clients: 9],
                              [port: 6082, clients: 25]] | 11       | 500
        []                 | [[port: 6060, clients: 17],
                              [port: 6081, clients: 9],
                              [port: 6082, clients: 25]] | 11       | 500
    }

    def "Server's broadcastOnAllChannels sends to all clients on a port"() {
        given:
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []

        def totalMessages = 0
        def totalClients = 0
        configurations.each { config ->
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver
                client.connect()
                client.registerChannelReadListener(clientReceiver)

                totalClients++

            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        def msg = TestUtil.createKonemMessage("send")
        configurations.each { config ->
            1.upto(messages) {
                server.broadcastOnAllChannels(msg)
            }
            totalMessages += (totalClients * messages)
        }

        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"

        totalMessages == clientMessagesRecieved
        println "-----------------------------"
        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 1]]  | 25       | 500
        [[port: 6060, clients: 33]] | 7        | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12]] | 25       | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29]] | 19       | 500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29]] | 75       | 2500
        [[port: 6060, clients: 33],
         [port: 6081, clients: 12],
         [port: 6082, clients: 29],
         [port: 6083, clients: 7]]  | 75       | 2500

    }

    def "Server can receive and then respond to correct clients"() {
        given:
        def serverReceiverList = []
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << serverReceiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, serverReceiver)
        }

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += (config.clients * (messages))
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver("client-$it-${config.port}", { addr, msg ->
                    clientReceiver.messageCount++
                    clientReceiver.messageList << msg
                })
                clientList << client
                clientReceiverList << clientReceiver
                client.registerChannelReadListener(clientReceiver)
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        for (JsonClient client : clientList) {
            client.receiveListeners.each { rdr ->
                1.upto(messages) {
                    client.sendMessage(TestUtil.createKonemMessage("${rdr.clientId}"))
                }
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, clientList.size() * messages, receiveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, clientList.size() * messages, receiveTime)

        then:
        clientList.each { JsonClient client ->
            clientReceiverList.each { rdr ->
                rdr.messageList.each { KonemMessage message ->
                    //println "${message.getData().data} == ${rdr.clientId}"
                    assert message.getData().data == rdr.clientId
                }
            }
        }

        where:
        configurations              | messages | receiveTime
        [[port: 6060, clients: 1]]  | 5        | 500
        [[port: 6060, clients: 10]] | 5        | 500
        [[port: 6060, clients: 10],
         [port: 6081, clients: 10]] | 5        | 500
        [[port: 6060, clients: 10],
         [port: 6081, clients: 10],
         [port: 6082, clients: 10],
         [port: 6083, clients: 10]] | 25       | 4000
    }

    def "Clients ConnectionListener is called after connect and able to send message"() {
        given:
        def serverReceiverList = []

        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.registerChannelReadListener(serverReceiver)
        serverReceiverList << serverReceiver

        def connectionList = []
        server.registerConnectionListener(new ConnectionListener({ addr ->
            connectionList << addr
        }))

        server.startServer()
        TestUtil.waitForServerActive(server)

        def msg = TestUtil.createKonemMessage("send")

        def clientList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += config.clients
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                client.registerConnectionListener(new ConnectionListener({ addr ->
                    client.sendMessage(msg)
                }))
                clientList << client
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, receiveTime)

        then:
        def serverMessages = serverReceiver.messageCount
        println "server recieved $serverMessages == $totalMessages client messages sent"
        assert serverMessages == totalMessages
        println "server connections ${connectionList.size()} == $totalMessages clients "
        def serverSize = connectionList.size()
        assert serverSize == totalMessages
        println "-----------------------------"
        where:
        configurations              | receiveTime
        [[port: 6060, clients: 1]]  | 5000
        [[port: 6060, clients: 50]] | 5000
        [[port: 6060, clients: 20],
         [port: 6081, clients: 20]] | 5000
        [[port: 6060, clients: 50],
         [port: 6081, clients: 50],
         [port: 6082, clients: 50],
         [port: 6083, clients: 50]] | 5000
    }

    def "Server's ConnectionListener is called after a client connects and able to send message to client"() {
        given:
        def message = TestUtil.createKonemMessage("send")
        def serverReceiverList = []

        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++

        })

        server.registerChannelReadListener(serverReceiver)
        serverReceiverList << serverReceiver

        def connectionList = []
        server.registerConnectionListener(new ConnectionListener({ addr ->
            connectionList << addr
            server.sendMessage(addr, message)
        }))

        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += config.clients
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                })
                clientList << client
                clientReceiverList << clientReceiver
                client.registerChannelReadListener(clientReceiver)
                client.connect()
            }
        }
        TestUtil.ensureClientsActive(clientList)
        when:
        print "Server "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, receiveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { JsonClient client ->
            def receivers = client.receiveListeners
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
        configurations              | receiveTime
        [[port: 6060, clients: 1]]  | 5000
        [[port: 6060, clients: 50]] | 5000
        [[port: 6060, clients: 20],
         [port: 6081, clients: 20]] | 5000
        [[port: 6060, clients: 50],
         [port: 6081, clients: 50],
         [port: 6082, clients: 50],
         [port: 6083, clients: 50]] | 5000
    }

    def "ConnectionStatusListener's connect and disconnect listeners are called"() {
        given:
        def message = TestUtil.createKonemMessage("send")
        def serverReceiverList = []
        def serverReceiver
        serverReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
            serverReceiver.messageCount++
        })

        server.registerChannelReadListener(serverReceiver)

        def connections = 0
        def disconnections = 0
        server.registerConnectionStatusListener(new ConnectionStatusListener(
                { addr ->
                    connections++
                    server.sendMessage(addr, message)
                },
                { addr ->
                    disconnections++
                }))

        serverReceiverList << serverReceiver
        server.startServer()
        TestUtil.waitForServerActive(server)

        def clientList = []
        def clientReceiverList = []
        def totalMessages = 0
        configurations.each { config ->
            totalMessages += config.clients
            1.upto(config.clients) {
                def client = factory.createClient("localhost", config.port)
                clientList << client
                def clientReceiver
                clientReceiver = new GroovyJsonMessageReceiver({ addr, msg ->
                    clientReceiver.messageCount++
                    client.disconnect()
                })
                client.registerChannelReadListener(clientReceiver)
                clientReceiverList << clientReceiver
                client.connect()

            }
        }
        TestUtil.ensureClientsActive(clientList)

        when:
        TestUtil.ensureDisconnected(clientList)
        Thread.sleep(receiveTime)
        then:
        println "ConnectionStatusListener saw connections: $connections  == ${clientList.size()} clients "
        assert connections == clientList.size()
        println "ConnectionStatusListener saw disconnections: $disconnections  == ${clientList.size()} clients "
        assert disconnections == clientList.size()
        println "-----------------------------"
        where:
        configurations              | receiveTime
        [[port: 6060, clients: 1]]  | 2000
        [[port: 6060, clients: 50]] | 2000
        [[port: 6060, clients: 20],
         [port: 6081, clients: 20]] | 2000
        [[port: 6060, clients: 25],
         [port: 6081, clients: 50],
         [port: 6082, clients: 25],
         [port: 6083, clients: 50]] | 2000

    }

}


