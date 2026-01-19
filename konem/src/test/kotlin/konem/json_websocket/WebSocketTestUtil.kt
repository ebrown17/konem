package konem.json_websocket

import konem.NewTestClientReceiver
import konem.NewTestServerReceiver
import konem.TestServerReceiver
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.client.Client
import konem.netty.client.WebSocketClientFactory
import konem.netty.server.WebSocketServer
import java.net.SocketAddress

var server: WebSocketServer<KonemMessage>? = null
var clientFactory: WebSocketClientFactory<KonemMessage>? = null

class JsonTestWebSocketServerReceiver(
    received: (SocketAddress, KonemMessage) -> Unit
) : NewTestServerReceiver<KonemMessage>(received)

class JsonTestWebSocketClientReceiver(
    client: Client<KonemMessage>, receive: (SocketAddress, KonemMessage) -> Unit
): NewTestClientReceiver<KonemMessage>(client,receive)

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
