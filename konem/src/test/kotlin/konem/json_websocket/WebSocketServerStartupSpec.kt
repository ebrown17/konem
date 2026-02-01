package konem.json_websocket

import io.kotest.assertions.nondeterministic.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.engine.concurrency.TestExecutionMode
import konem.DEBUG
import konem.Konem
import konem.WebSocketServerStartup
import konem.activeTime
import konem.protocol.konem.KonemProtocolPipeline
import konem.startServer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalKotest
class WebSocketServerStartupSpec: FunSpec({
    testExecutionMode = TestExecutionMode.Sequential

    afterTest{
        clientFactory?.shutdown()
        server?.shutdownServer()
        delay(500.milliseconds)
    }

    context(": WebSocket Server starts with expected ports and websocket paths"){
        withTests(
            nameFn = {  data: WebSocketServerStartup -> "${this.testCase.name.name} ${data.portsToWebSocketPaths}"  },
            ts = listOf(
                WebSocketServerStartup(mutableMapOf(6060 to mutableListOf("/test"))),
                WebSocketServerStartup(mutableMapOf(
                    6060 to mutableListOf("/test","/test1"),
                )),
                WebSocketServerStartup(mutableMapOf(
                    6060 to mutableListOf("/test","/test1","/test2","/test3"),
                )),
                WebSocketServerStartup(mutableMapOf(
                    6060 to mutableListOf("/test","/test1","/test2","/test3"),
                    6061 to mutableListOf("/test4","/test5","/test6","/test7"),
                )),
                WebSocketServerStartup(mutableMapOf(
                    6060 to mutableListOf("/test","/test1","/test2","/test3"),
                    6061 to mutableListOf("/test4","/test5","/test6","/test7"),
                    6062 to mutableListOf("/test8","/test9","/test10","/test11"),
                    6063 to mutableListOf("/test12","/test13","/test14","/test15"),
                )),
                WebSocketServerStartup(mutableMapOf(
                    6060 to mutableListOf("/test","/test1"),
                    6061 to mutableListOf("/test2","/test3","/test6","/test4"),
                    6062 to mutableListOf("/test8","/test7","/test5",),
                    6063 to mutableListOf("/test9","/test13"),
                )),
            ),
        ){ (portsToWebSocketPaths) ->


            var ports = mutableSetOf<Int>()
            server = Konem.createWebSocketServer(
                config = {
                    portsToWebSocketPaths.forEach { (port, wsPaths) ->
                        ports.add(port)
                        addChannel(port,*wsPaths.toTypedArray())
                    }
                },
                protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
            )

            startServer(server!!)
            delay(1.seconds)
            until(activeTime.seconds){
                var allPortsActive = false
                var allPathsConfigured = true
                server?.let {
                    allPortsActive = it.allActive()
                    portsToWebSocketPaths.forEach { (port, paths) ->
                        paths.forEach { path ->
                            if(!it.isPathConfigured(port,path)){
                                allPathsConfigured = false
                            }
                        }
                    }
                }

                println("All ports $ports are active: $allPortsActive")
                println("All paths are configred: $allPathsConfigured")
                allPortsActive && allPathsConfigured
            }
            if (DEBUG) println("-----------------------------------")
        }
    }
})
