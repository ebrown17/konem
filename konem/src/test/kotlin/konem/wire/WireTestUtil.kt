package konem.wire


import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import konem.TestClientReceiver
import konem.TestServerReceiver
import konem.data.protobuf.Data
import konem.data.protobuf.KonemMessage
import konem.data.protobuf.MessageType
import konem.netty.client.Client
import konem.netty.client.TcpSocketClientFactory
import konem.netty.server.Server
import konem.netty.server.TcpSocketServer
import konem.protocol.tcp.TcpClientFactory
import konem.waitForMsgTime
import java.net.SocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

var server: TcpSocketServer<KonemMessage>? = null
var clientFactory:  TcpSocketClientFactory<KonemMessage>? = null


class WireTestClientReceiver(client: Client<KonemMessage>, receive: (SocketAddress, KonemMessage) -> Unit):
    TestClientReceiver<KonemMessage>(client,receive)

class WireTestServerReceiver(receive: (SocketAddress, KonemMessage) -> Unit): TestServerReceiver<KonemMessage>(receive)

fun sendClientMessages(messageSendCount: Int, clientList: MutableList<Client<KonemMessage>>):Int{
    var totalMessagesSent = 0
    clientList.forEachIndexed { index, client ->
        for(i in 1..messageSendCount){
            totalMessagesSent++
            client.sendMessage(
                KonemMessage(
                    messageType = MessageType.DATA,
                    data = Data("Client $index message $i")
                )
            )
        }
    }
    return totalMessagesSent
}

fun sendClientMessageWithReceiver(messageSendCount: Int, clientList: MutableList<WireTestClientReceiver>):Int{
    var totalMessagesSent = 0
    clientList.forEach{ receiver ->
        for(i in 1..messageSendCount){
            totalMessagesSent++
            receiver.client.sendMessage(
                KonemMessage(
                    messageType = MessageType.DATA,
                    data = Data(receiver.clientId)
                )
            )
        }
    }
    return totalMessagesSent
}

fun serverBroadcastOnChannels(messageSendCount: Int, broadcastPorts: MutableList<Int>){
    broadcastPorts.forEach { port ->
        for(i in 1..messageSendCount){
            server?.broadcastOnChannel(
                port,
                KonemMessage(
                    messageType = MessageType.DATA,
                    data = Data("Server message $i")
                )
            )
        }
    }
}

fun serverBroadcastOnAllChannels(messageSendCount: Int){
    for(i in 1..messageSendCount){
        server?.broadcastOnAllChannels(
            KonemMessage(
                messageType = MessageType.DATA,
                data = Data("Server message $i")
            )
        )
    }
}

@ExperimentalTime
suspend fun waitForMessagesReceiverClient(totalMessages:Int ,receiverList : MutableList<WireTestClientReceiver>,debug: Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        val received: Int = receiverList.sumOf { it.messageCount }
        var correctMsgs = true
        receiverList.forEach{ receiver ->
            receiver.messageList.forEach {
                val msg = it.data as Data
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
