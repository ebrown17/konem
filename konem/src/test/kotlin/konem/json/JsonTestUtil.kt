package konem.json


import io.kotest.assertions.nondeterministic.until
import konem.TestClientReceiver
import konem.TestServerReceiver
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.ConnectionKey
import konem.netty.client.Client
import konem.netty.client.TcpSocketClientFactory
import konem.netty.server.TcpSocketServer
import konem.waitForMsgTime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

var server: TcpSocketServer<KonemMessage>? = null
var clientFactory:  TcpSocketClientFactory<KonemMessage>? = null


class JsonTestServerReceiver(
    received: (ConnectionKey, KonemMessage) -> Unit
) : TestServerReceiver<KonemMessage>(received)

class JsonTestClientReceiver(
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
    until(waitForMsgTime.seconds) {
        val received: Int = receiverList.sumOf { it.messageCount.get() }
        var correctMsgs = true
        receiverList.forEach{ receiver ->
            receiver.messageListByConnection.values.forEach { connectionMessages ->
                connectionMessages.forEach {
                    val msg = it.message as Data
                    if(msg.data != receiver.clientId){
                        correctMsgs = false
                    }
                }
            }
        }
        if(debug){
            println("Clients received: $received out of $totalMessages")
        }
        (received == totalMessages) && correctMsgs
    }
    return true
}
