package konem.protocol.wire

import konem.testUtil.GroovyWireMessageReciever
import konem.testUtil.TestUtil
import spock.lang.Shared
import spock.lang.Specification

class WireCommunicationSpec  extends Specification {
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
        def  receiver
         receiver =  new GroovyWireMessageReciever({ addr, msg ->
            receiver.messageCount++
        })

        configurations.each { config ->
            server.registerChannelReadListener(config.port, receiver)
        }

        server.startServer()
        Thread.sleep(2000)

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
        TestUtil.waitForAllMessages(receiver,totalMessages,recieveTime)

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

}


