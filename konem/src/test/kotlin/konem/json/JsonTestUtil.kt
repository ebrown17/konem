package konem.json


import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import konem.TestClientReceiver
import konem.TestServerReceiver
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.client.Client
import konem.netty.server.Server
import konem.protocol.tcp.TcpClientFactory
import konem.waitForMsgTime
import java.net.SocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

var server: Server<KonemMessage>? = null
var clientFactory:  TcpClientFactory<KonemMessage>? = null


class JsonTestClientReceiver(client: Client<KonemMessage>, receive: (SocketAddress, KonemMessage) -> Unit):
    TestClientReceiver<KonemMessage>(client,receive)

class JsonTestServerReceiver(receive: (SocketAddress, KonemMessage) -> Unit): TestServerReceiver<KonemMessage>(receive)

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

fun sendClientMessageWithReceiver(messageSendCount: Int, clientList: MutableList<JsonTestClientReceiver>):Int{
    var totalMessagesSent = 0
    clientList.forEach{ receiver ->
        for(i in 1..messageSendCount){
            totalMessagesSent++
            receiver.client.sendMessage(KonemMessage(message = Data(receiver.clientId) ))
        }
    }
    return totalMessagesSent
}

fun serverBroadcastOnChannels(messageSendCount: Int, broadcastPorts: MutableList<Int>){
    broadcastPorts.forEach { port ->
        for(i in 1..messageSendCount){
            server?.broadcastOnChannel(port,KonemMessage(message = Data("Server message $i")))
        }
    }
}

fun serverBroadcastOnAllChannels(messageSendCount: Int){
    for(i in 1..messageSendCount){
        server?.broadcastOnAllChannels(KonemMessage(message = Data("Server message $i")))
    }
}

@ExperimentalTime
suspend fun waitForMessagesReceiverClient(totalMessages:Int ,receiverList : MutableList<JsonTestClientReceiver>,debug: Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        val received: Int = receiverList.sumOf { it.messageCount }
        var correctMsgs = true
        receiverList.forEach{ receiver ->
            receiver.messageList.forEach {
                val msg = it.message as Data
                if(msg.data != receiver.clientId){
                    correctMsgs = false
                }
            }
        }
        if(debug){
            println("Clients received: $received out of $totalMessages")
        }
        (received == totalMessages) && correctMsgs
    }
}
