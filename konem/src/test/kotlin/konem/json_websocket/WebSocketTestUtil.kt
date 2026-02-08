package konem.json_websocket

import konem.TestClientReceiver
import konem.TestServerReceiver
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.ConnectionKey
import konem.netty.client.Client
import konem.netty.client.WebSocketClientFactory
import konem.netty.server.WebSocketServer

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
