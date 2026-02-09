package konem.json_websocket

import konem.connectClients
import konem.TestClientReceiver
import konem.TestConnectionListener
import konem.TestServerReceiver
import konem.WsClientConfig
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.ConnectionKey
import konem.netty.client.Client
import konem.netty.client.WebSocketClientFactory
import konem.netty.server.WebSocketServer
import konem.waitForServerStatusChange
import kotlin.time.ExperimentalTime

var server: WebSocketServer<KonemMessage>? = null
var clientFactory: WebSocketClientFactory<KonemMessage>? = null

class JsonTestWebSocketServerReceiver(
    received: (ConnectionKey, KonemMessage) -> Unit
) : TestServerReceiver<KonemMessage>(received)

class JsonTestWebSocketClientReceiver(
    client: Client<KonemMessage>, receive: (ConnectionKey, KonemMessage) -> Unit
): TestClientReceiver<KonemMessage>(client,receive)

fun sendClientMessages(messageSendCount: Int, clientList: MutableList<Client<KonemMessage>>):Int{
    var totalMessagesSent = 0
    clientList.forEachIndexed { index, client ->
        for(i in 1..messageSendCount){
            totalMessagesSent++
            client.sendMessage(KonemMessage(message = Data("Client $index message $i") ))
        }
    }
    return totalMessagesSent
}
fun sendClientMessagesCounted(messageSendCount: Int, clientList: MutableList<Client<KonemMessage>>):Int{
    var totalMessagesSent = 0
    clientList.forEach{client ->
        for(i in 0..messageSendCount){
            totalMessagesSent++
            client.sendMessage(KonemMessage(message = Data("$i") ))
        }
    }
    return totalMessagesSent
}

fun countExpectedWebSocketConnections(clientConfigs: List<WsClientConfig>): Int {
    return clientConfigs.sumOf { config -> config.totalClients * config.paths.size }
}

@ExperimentalTime
suspend fun connectClientsAndWaitForServerConnections(
    clientList: MutableList<Client<KonemMessage>>,
    expectedConnections: Int,
    debug: Boolean = false
): Boolean {
    lateinit var serverConnectionListener: TestConnectionListener
    serverConnectionListener = TestConnectionListener {
        serverConnectionListener.connections++
    }
    val configuredServer = requireNotNull(server) { "WebSocket test server must be initialized before connecting clients" }
    configuredServer.registerConnectionListener(serverConnectionListener)
    connectClients(clientList)
    waitForServerStatusChange(expectedConnections, mutableListOf(serverConnectionListener), debug, true)
    return true
}
