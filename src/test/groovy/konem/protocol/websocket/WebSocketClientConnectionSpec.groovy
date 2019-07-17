package konem.protocol.websocket

import spock.lang.IgnoreIf
import spock.lang.Shared
import spock.lang.Specification
import konem.testUtil.TestUtil

/**
 * + clients can connect to all open ports
 * + clients can connect to all ws paths on ports
 * + clients can disconnect correctly
 * + clients can reconnect after disconnect called
 * - clients auto reconnect if unexpected disconnect ( can't test without simulating network disconnect)
 * + client connection retry time is calc'd correctly
 * - only one connection attempt is registered (debounce connect calls)
 * - connect logic can't be initiated if already connected
 * + trying to connect to a port with a ws path that's not configured returns properly
 * -
 */

class WebSocketClientConnectionSpec extends Specification {

    @Shared
    WebSocketServer server

    @Shared
    WebSocketClientFactory factory

    def setup() {
        server = new WebSocketServer()
        server.addChannel(6060, "/test0", "/test")
        server.addChannel(6081, "/test1", "/test2")
        server.addChannel(6082, "/test3", "/test4", "/test5")
        server.addChannel(6083, "/test6", "/test7", "/test8", "/test9",
                "/test10", "/test11", "/test12", "/test13", "/test14", "/test15")

        factory = new WebSocketClientFactory()
    }

    def cleanup() {
        server.shutdownServer()
        factory.shutdown()
        server = null
        factory = null
    }


    def "All clients can connect to already started server"() {
        given:
        server.startServer()
        def clientMap = [:]

        def clientList = []

        configurations.each { config ->
            clientMap[config.port] = []
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    clientMap[config.port] << client
                    clientList << client
                }
            }
        }

        when:

        clientList.each { client ->
            client.connect()
        }

        TestUtil.ensureClientsActive(clientList)

        then:

        clientList.each { WebSocketClient client ->
            assert client.isActive()
        }

        where:
        configurations                                                     | _
        [[port: 6060, paths: ["/test"], clients: 1]]                       | _
        [[port: 6060, paths: ["/test"], clients: 15]]                      | _
        [[port: 6060, paths: ["/test0", "/test"], clients: 3]]             | _
        [[port: 6060, paths: ["/test0", "/test"], clients: 39]]            | _
        [[port: 6060, paths: ["/test"], clients: 11],
         [port: 6081, paths: ["/test1", "/test2"], clients: 15]]           | _
        [[port: 6060, paths: ["/test"], clients: 11],
         [port: 6081, paths: ["/test1", "/test2"], clients: 15],
         [port: 6082, paths: ["/test3", "/test4", "/test5"], clients: 15]] | _
        [[port: 6060, paths: ["/test"], clients: 11],
         [port: 6081, paths: ["/test1", "/test2"], clients: 15],
         [port: 6082, paths: ["/test3", "/test4", "/test5"], clients: 15],
         [port: 6083, paths: ["/test6", "/test7", "/test8", "/test9",
                              "/test10", "/test11", "/test12", "/test13",
                              "/test14", "/test15"], clients: 15]]         | _
    }

    def "Clients disconnect as expected"() {
        given:
        server.startServer()

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

        when:
        TestUtil.ensureClientsActive(clientList)

        clientList.each { WebSocketClient client ->
            client.disconnect()
        }

        TestUtil.ensureDisconnected(clientList)

        then:

        clientList.each { WebSocketClient client ->
            assert !client.isActive()
        }

        where:
        configurations                                          | sleepTime
        [[port: 6060, paths: ["/test"], clients: 1]]            | 500
        [[port: 6060, paths: ["/test"], clients: 10]]           | 500
        [[port: 6060, paths: ["/test0", "/test"], clients: 10]] | 500

    }

    def "Clients can reconnect after disconnect called"() {
        given:
        server.startServer()
        def clientList = []

        configurations.each { config ->
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

        1.upto(disconnects) {
            Thread.sleep(sleepTime)


            clientList.each { WebSocketClient client ->
                client.disconnect()
            }

            TestUtil.ensureDisconnected(clientList)


            clientList.each { WebSocketClient client ->
                client.connect()
            }

            TestUtil.ensureClientsActive(clientList)
        }
        then:

        clientList.each { WebSocketClient client ->
            //println client.toString()
            assert client.isActive()
        }


        where:
        configurations                                           | sleepTime | disconnects
        [[port: 6060, paths: ["/test"], clients: 1]]             | 500       | 1
        [[port: 6060, paths: ["/test"], clients: 10]]            | 500       | 3
        [[port: 6060, paths: ["/test0", "/test"], clients: 10]]  | 500       | 1
        [[port: 6060, paths: ["/test0", "/test"], clients: 10]]  | 500       | 3
        [[port: 6060, paths: ["/test"], clients: 9],
         [port: 6081, paths: ["/test1", "/test2"], clients: 13]] | 500       | 1
        [[port: 6060, paths: ["/test"], clients: 11],
         [port: 6081, paths: ["/test1", "/test2"], clients: 15]] | 900       | 3

    }

    @IgnoreIf({System.getProperty('SKIP.LONG') == '1'} )
    def "Clients retry time is calculated correctly"() {
        given:
        def clientList = []

        configurations.each { config ->
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    client.connect()
                    clientList << client
                }
            }
            Thread.sleep(1000)
        }

        when:

        Thread.sleep(waitTime)

        then:

        clientList.each { WebSocketClient client ->
            println "retries: ${client.retryCount} time retrying: ${(System.currentTimeMillis() - client.retryTime) / 1000L}"
            assert client.getRetryCount() == retryCount
        }


        where:
        configurations                                             | waitTime | retryCount
        [[port: 6060, paths: ["/test"], clients: 1]]               | 1500     | 1
        [[port: 6060, paths: ["/test"], clients: 1]]               | 15000    | 2
        [[port: 6060, paths: ["/test"], clients: 1]]               | 22500    | 3
    }

    def "Connecting to a port with a ws path that's not configured returns properly"() {
        given:
        server.startServer()
        def clientList = []

        configurations.each { config ->
            config.paths.each { path ->
                1.upto(config.clients) {
                    def client = factory.createClient("localhost", config.port, path)
                    clientList << client
                }
            }
        }
        Thread.sleep(sleepTime)
        when:

        clientList.each { WebSocketClient client ->
            client.connect()
        }

        Thread.sleep(1500)
        then:

        clientList.each { WebSocketClient client ->
            assert !client.isActive()
        }

        where:
        configurations                                  | sleepTime
        [[port: 6060, paths: ["/test11"], clients: 1]]  | 500
        [[port: 6060, paths: ["/test11"], clients: 10]] | 500
    }

}
