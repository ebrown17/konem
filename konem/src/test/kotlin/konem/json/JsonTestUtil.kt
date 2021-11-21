package konem.json

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import konem.data.json.Data
import konem.data.json.KonemMessage
import konem.netty.stream.ConnectionListener
import konem.netty.stream.ConnectionStatusListener
import konem.netty.stream.DisconnectionListener
import konem.netty.stream.StatusListener
import konem.protocol.socket.json.JsonClient
import konem.protocol.socket.json.JsonClientFactory
import konem.protocol.socket.json.JsonServer
import konem.protocol.socket.json.KonemMessageReceiver
import java.net.SocketAddress
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

var server: JsonServer? = null
var clientFactory:  JsonClientFactory? = null

const val DEBUG = true
const val activeTime = 3
const val waitForMsgTime = 3
const val delayDurationMs = 1L

val testSetup = {
    clientFactory?.shutdown()
    server?.shutdownServer()

    server = JsonServer()
    server?.let {
        it.addChannel(6060)
        it.addChannel(6061)
        it.addChannel(6062)
        it.addChannel(6063)
    }

    clientFactory = JsonClientFactory()
}

data class ClientConfig(val port: Int,val totalClients: Int)
data class ClientCommConfigsV1(val msgCount:Int, val clientConfigs: MutableList<ClientConfig>)
data class ClientCommConfigsV2(val msgCount:Int, val broadcastPorts: MutableList<Int>, val clientConfigs: MutableList<ClientConfig>)
data class ServerStartup(val totalConfigured: Int ,val portsToConfigure: MutableList<Int>)

class JsonTestServerReceiver(receive: (SocketAddress, KonemMessage) -> Unit) : KonemMessageReceiver(receive) {
    var messageCount = 0
    var messageList = mutableListOf<KonemMessage>()
}

class JsonTestClientReceiver(var client: JsonClient, receive: (SocketAddress, KonemMessage) -> Unit) : KonemMessageReceiver(receive) {
    var messageCount = 0
    var messageList = mutableListOf<KonemMessage>()
    var clientId = ""
}

class TestConnectionListener(connected: (SocketAddress) -> Unit): ConnectionListener(connected){
    var connections = 0
}

class TestDisconnectionListener(disconnected: (SocketAddress) -> Unit) : DisconnectionListener(disconnected) {
    var disconnections = 0
}

class TestConnectionStatusListener(connected: (SocketAddress) -> Unit,
                                   disconnected: (SocketAddress) -> Unit
) : ConnectionStatusListener(connected, disconnected) {
    var connections = 0
    var disconnections = 0
}

fun areClientsActive(clientList: MutableList<JsonClient>):Boolean{
    if(clientList.isEmpty()) return false
    var allActive = true
    clientList.forEach { client ->
        if(!client.isActive()){
            allActive = false
        }
    }
    return allActive
}

fun areClientsInactive(clientList: MutableList<JsonClient>):Boolean{
    if(clientList.isEmpty()) return true
    var allInactive = true
    clientList.forEach { client ->
        if(client.isActive()){
            allInactive = false
        }
    }
    return allInactive
}

fun sendClientMessages(messageSendCount: Int, clientList: MutableList<JsonClient>):Int{
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
suspend fun startServer() : Boolean{
    server?.startServer()!!

    return  until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
        server?.allActive()!!
    }
}

@ExperimentalTime
suspend fun connectClients(clientList : MutableList<JsonClient>) : Boolean{
    clientList.forEach { client -> client.connect() }

    return until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
        areClientsActive(clientList)
    }
}

@ExperimentalTime
suspend fun disconnectClients(clientList : MutableList<JsonClient>) : Boolean{
    clientList.forEach { client -> client.disconnect() }

    return until(Duration.seconds(activeTime), Duration.milliseconds(250).fixed()) {
        areClientsInactive(clientList)
    }
}

@ExperimentalTime
suspend fun waitForMessagesServer(totalMessages:Int ,receiverList : MutableList<JsonTestServerReceiver>,debug: Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        val received: Int = receiverList.sumOf { it.messageCount }
        if(debug){
            println("Server received: $received out of $totalMessages")
        }
        received == totalMessages
    }
}

@ExperimentalTime
suspend fun waitForMessagesClient(totalMessages:Int ,receiverList : MutableList<JsonTestClientReceiver>,debug: Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        val received: Int = receiverList.sumOf { it.messageCount }
        if(debug){
            println("Clients received: $received out of $totalMessages")
        }
        received == totalMessages
    }
}

@ExperimentalTime
suspend fun waitForClientStatusChange(totalChanges:Int, list : MutableList<out StatusListener>, debug: Boolean = false, checkConnect:Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        var type = ""
        val received: Int = list.sumOf { statusChange ->
            when(statusChange){
                is TestConnectionListener -> { type = "Connections" ; statusChange.connections    }
                is TestDisconnectionListener -> { type = "Disconnections"; statusChange.disconnections  }
                is TestConnectionStatusListener -> {
                    if(checkConnect) {
                        type = "Connections" ; statusChange.connections
                    }
                    else{
                        type = "Disconnections"; statusChange.disconnections
                    }
                }
                else -> 0
            }
        }
        if(debug){
            println("Client $type received: $received out of $totalChanges")
        }
        received == totalChanges
    }
}

@ExperimentalTime
suspend fun waitForServerStatusChange(totalChanges:Int ,list : MutableList<out StatusListener>,debug: Boolean = false,checkConnect:Boolean = false) : Boolean{
    return until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
        var type = ""
        val received: Int = list.sumOf { statusChange ->
            when(statusChange){
                is TestConnectionListener -> { type = "Connections" ; statusChange.connections    }
                is TestDisconnectionListener -> { type = "Disconnections"; statusChange.disconnections  }
                is TestConnectionStatusListener -> {
                    if(checkConnect) {
                        type = "Connections" ; statusChange.connections
                    }
                    else{
                        type = "Disconnections"; statusChange.disconnections
                    }
                }
                else -> 0
            }
        }
        if(debug){
            println("Server $type received: $received out of $totalChanges")
        }
        received == totalChanges
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
