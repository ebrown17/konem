package konem.protocol.socket.json

import konem.testUtil.TestUtil
import spock.lang.Shared
import spock.lang.Specification

class JsonServerStartupSpec extends Specification {

    @Shared
    JsonServer server

    def setup() {
        server = new JsonServer()
    }

    def cleanup() {
        server.shutdownServer()
        server = null
    }

    def "Started with expected channel values"() {

        when:
        for (i in ports) {
            int port = i
            server.addChannel(port)
        }
        server.startServer()
        TestUtil.waitForServerActive(server)
        then:
        def portsConfigured = true
        ports.each { port ->
            if (!server.isPortConfigured(port)) {
                portsConfigured = false
            }
        }

        def transMap = server.getTransceiverMap()
        def transConfigured = true
        transMap.each { port, tran ->
            if (!ports.contains(port)) {
                transConfigured = false
            }

        }

        assert actualTotal == transMap.size()
        assert portsConfigured
        assert transConfigured

        where:
        ports                                | actualTotal
        [6060]                               | 1
        [6060, 6081]                         | 2
        [6060, 6081, 9000]                   | 3
        [6060, 6081, 9000, 7000, 7001, 8888] | 6
        [6060, 6081, 6081, 6082, 6082, 6081] | 3
        [6060, 6060, 6060, 6060, 6060, 6081] | 2

    }

}