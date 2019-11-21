package konem.protocol.websocket

import konem.protocol.websocket.json.WebSocketServer
import konem.testUtil.TestUtil
import spock.lang.Shared
import spock.lang.Specification

class WebSocketServerStartupSpec extends Specification {

    @Shared
    WebSocketServer server

    def setup() {
        server = new WebSocketServer()
    }

    def cleanup() {
        server.shutdownServer()
        server = null
    }

    def "Started with expected channel values"() {

        when:
        for (i in ports) {
            int port = i
            server.addChannel(port, "/${port}")
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

    def "Server detects duplicate ports and channels"() {
        when:
        def count = 0
        for (i in ports) {
            int port = i
            server.addChannel(port, paths[count++] as String[])
        }
        server.startServer()
        TestUtil.waitForServerActive(server)
        then:
        def transMap = server.getTransceiverMap()
        def websockMap = server.websocketMap
        def total = 0
        websockMap.each { port, mapSet ->
            total += mapSet.size()
        }
        assert transTotal == transMap.size()
        assert wsTotal == total

        where:
        ports              | paths                                        | transTotal | wsTotal
        [6060]             | [["/test"]]                                  | 1          | 1
        [6060]             | [["/test", "/test1"]]                        | 1          | 2
        [6060]             | [["/test", "/test1", "/test2", "/test3"]]    | 1          | 4
        [6060]             | [["/test", "/test1", "/test", "/test1"]]     | 1          | 2
        [6060]             | [["/test", "/test1", "/test1", "/test1"]]    | 1          | 2
        [6060, 6081]       | [["/test"], ["/tes1"]]                       | 2          | 2
        [6060, 6081]       | [["/test", "/test2"], ["/tes1"]]             | 2          | 3
        [6060, 6081]       | [["/test", "/test1", "/test2", "/test3"],
                              ["/test4", "/test5", "/test6", "/test7"]]   | 2          | 8
        [6060, 6081]       | [["/test", "/test1", "/test2", "/test3"],
                              ["/test5", "/test5", "/test6", "/test7"]]   | 2          | 7
        [6060, 6081]       | [["/test", "/test1", "/test2", "/test2"],
                              ["/test", "/test5", "/test6", "/test7"]]    | 2          | 7
        [6060, 6081]       | [["/test", "/test2", "/test2", "/test2"],
                              ["/test", "/test2", "/test2", "/test7"]]    | 2          | 5
        [6060, 6081]       | [["/test", "/test1", "/test2", "/test3"],
                              ["/test", "/test", "/test", "/test"]]       | 2          | 5
        [6060, 6081, 6081] | [["/test", "/test1", "/test2", "/test3"],
                              ["/test", "/test", "/test", "/test"],
                              ["/test", "/test", "/test", "/test"]]       | 2          | 5
        [6060, 6081]       | [["/test"],
                              ["/test", "/test1", "/test", "/test"]]      | 2          | 3
        [6060, 6081]       | [[],
                              ["/test", "/test1", "/test", "/test"]]      | 1          | 2
        [6060, 6081,
         6083, 6084]       | [["/test"], ["/test1"],
                              ["/test2"], ["/test3"]]                     | 4          | 4
        [6060, 6081,
         6083, 6084]       | [["/test", "/test4"], ["/test1", "/test5"],
                              ["/test2", "/test6"], ["/test3", "/test4"]] | 4          | 8
        [6060, 6081,
         6083, 6084]       | [["/test", "/test4"], ["/test1", "/test5"],
                              ["/test2", "/test2"], ["/test5", "/test5"]] | 4          | 6
    }


    def "Websocket paths are assigned to correct channel"() {
        given:
        for (config in configurations) {
            def port = config.port
            def paths = config.paths.clone()
            server.addChannel(port, paths as String[])
        }
        server.startServer()
        TestUtil.waitForServerActive(server)
        when:
        def websockMap = server.websocketMap

        def matched = [matched: true]
        configurations.each { config ->
            def port = config.port
            def paths = config.paths as String[]
            def serverPaths = websockMap.get(port)
            paths.sort()
            serverPaths.sort()

            println "$serverPaths --- $paths"
            if (serverPaths != paths) {
                matched.matched = false
                matched.serverPaths = serverPaths
                matched.paths = paths
            }

        }
        then:
        assert matched.matched

        where:
        configurations                                                             | _
        [[port: 6060, paths: ["/test"]]]                                           | _
        [[port: 6060, paths: ["/test", "/test1", "/test2"]]]                       | _
        [[port: 6060, paths: ["/test", "/test1", "/test2"]],
         [port: 6081, paths: ["/test3", "/test4", "/test5"]]]                      | _
        [[port: 6060, paths: ["/test", "/test1", "/test2"]],
         [port: 6081, paths: ["/test3", "/test5"]],
         [port: 6082, paths: ["/test6", "/test7", "/test8", "/test9", "/test10"]]] | _
    }

}
