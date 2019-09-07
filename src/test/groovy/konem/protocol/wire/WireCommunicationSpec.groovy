package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.testUtil.GroovyWireMessageReciever
import konem.testUtil.TestUtil
import spock.lang.Shared
import spock.lang.Specification

class WireCommunicationSpec extends Specification {
    @Shared
    WireServer server

    @Shared
    WireClientFactory factory

    def setup() {
        server = new WireServer()
        server.addChannel(6060)
        server.addChannel(6081)
        server.addChannel(6082)
        server.addChannel(6083)

        factory = new WireClientFactory()
    }

    def cleanup() {
        server.shutdownServer()
        factory.shutdown()
    }

    def "Server readers can register before server starts and then see messages"() {
        given:
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
        })

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        TestUtil.waitForAllMessages(receiver, totalMessages, recieveTime)

        then:

        receiver.messageCount == totalMessages

        where:
        configurations              | messages | recieveTime
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

    def "Server readers can register after server starts and then see messages"() {
        given:
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
        })

        server.startServer()

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        TestUtil.waitForAllMessages(receiver, totalMessages, recieveTime)

        then:

        receiver.messageCount == totalMessages

        where:
        configurations              | messages | recieveTime
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
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << receiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
                clientReceiver = new GroovyWireMessageReciever({ addr, msg ->
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

        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)
        then:

        def clientMessagesRecieved = 0
        clientList.each { WireClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${receiver.messageCount} == $clientMessagesRecieved client messages"
        assert receiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | recieveTime
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
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << receiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
                clientReceiver = new GroovyWireMessageReciever({ addr, msg ->
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

        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)
        then:

        def clientMessagesRecieved = 0
        clientList.each { WireClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${receiver.messageCount} == $clientMessagesRecieved client messages"
        assert receiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | recieveTime
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
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << receiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
                clientReceiver = new GroovyWireMessageReciever({ addr, msg ->
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

        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)

        clientList.each { WireClient client ->
            client.disconnect()
        }
        TestUtil.ensureDisconnected(clientList)

        clientList.each { WireClient client ->
            client.connect()
        }

        TestUtil.ensureClientsActive(clientList)

        clientList.eachWithIndex { WireClient client, index ->
            1.upto(messages) {
                String data = "Client $index message $it"
                client.sendMessage(TestUtil.createWireMessage(data))
            }
        }
        totalMessages += totalMessages
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, totalMessages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)

        then:

        def clientMessagesRecieved = 0
        clientList.each { WireClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server recieved ${receiver.messageCount} == $clientMessagesRecieved client messages"
        assert receiver.messageCount == clientMessagesRecieved
        assert clientMessagesRecieved == totalMessages

        println "-----------------------------"
        where:
        configurations              | messages | recieveTime
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
                clientReceiver = new GroovyWireMessageReciever({ addr, msg ->
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
                server.broadcastOnAllChannels(TestUtil.createWireMessage("Server message $it"))
            }
        } else {
            broadcastPorts.each { port ->
                1.upto(messages) {
                    server.broadcastOnChannel(port, TestUtil.createWireMessage("Server message $it"))
                }
            }
        }

        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { WireClient client ->
            def receivers = client.readListeners
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"
        assert totalMessages == clientMessagesRecieved
        println "-----------------------------"
        where:
        broadcastPorts     | configurations              | messages | recieveTime
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
                clientReceiver = new GroovyWireMessageReciever({ addr, msg ->
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
        def msg = TestUtil.createWireMessage("send")
        configurations.each { config ->
            1.upto(messages) {
                server.broadcastOnAllChannels(msg)
            }
            totalMessages += (totalClients * messages)
        }

        TestUtil.waitForAllMessages(clientReceiverList, totalMessages, recieveTime)

        then:
        def clientMessagesRecieved = 0
        clientList.each { WireClient client ->
            def receivers = client.getReadListeners()
            receivers.each {
                clientMessagesRecieved += it.messageCount
            }
        }

        println "server sent $totalMessages == $clientMessagesRecieved client messages recieved"

        totalMessages == clientMessagesRecieved
        println "-----------------------------"
        where:
        configurations             | messages | recieveTime
        [[port: 6060, clients: 1]] | 5        | 500
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
        def receiver
        receiver = new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
            server.sendMessage(addr, msg)
        })
        serverReceiverList << receiver

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
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
                clientReceiver = new GroovyWireMessageReciever("client-$it-${config.port}",{ addr, msg ->
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
        for(WireClient client :clientList){
            client.getReadListeners().each { rdr ->
                1.upto(messages) {
                    client.sendMessage(TestUtil.createWireMessage("${rdr.clientId}"))
                }
            }
        }
        print "Server "
        TestUtil.waitForAllMessages(serverReceiverList, clientList.size() * messages, recieveTime)
        print "Client "
        TestUtil.waitForAllMessages(clientReceiverList, clientList.size() * messages, recieveTime)

        then:
        clientList.each { WireClient client ->
            def readers = client.readListeners
            clientReceiverList.each { rdr ->
                rdr.messageList.each {  KonemMessage message ->
                    //println "${message.getData().data} == ${rdr.clientId}"
                    assert message.getData().data == rdr.clientId
                }
            }
        }

        where:
        configurations             | messages | recieveTime
        [[port: 6060, clients: 1]] | 5        | 500
        [[port: 6060, clients: 10]] | 5        | 500
        [[port: 6060, clients: 10],
         [port: 6081, clients: 10]] | 5        | 500
        [[port: 6060, clients: 10],
         [port: 6081, clients: 10],
         [port: 6082, clients: 10],
         [port: 6083, clients: 10]] | 25        | 4000
    }

    // def "Clients ConnectionListener is called after connect and able to send message"() {}

    //  def "Server's ConnectionListener is called after a client connects and able to send message to client"() { }

    //  def "ConnectionStatusListener's connect and disconnect listeners are called"() { }

}


