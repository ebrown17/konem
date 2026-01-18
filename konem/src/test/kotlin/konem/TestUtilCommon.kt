package konem

import io.kotest.assertions.nondeterministic.until
import konem.netty.*
import konem.netty.client.Client
import konem.netty.server.Server
import java.net.SocketAddress
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

const val DEBUG = true
const val activeTime = 3
const val waitForMsgTime = 3
const val delayDurationMs = 1L

data class ClientConfig(val port: Int,val totalClients: Int)
data class ClientCommConfigsV1(val msgCount:Int, val clientConfigs: MutableList<ClientConfig>)
data class ClientCommConfigsV2(val msgCount:Int, val broadcastPorts: MutableList<Int>, val clientConfigs: MutableList<ClientConfig>)
data class ServerStartup(val portsToConfigure: MutableList<Int>)


data class WsClientConfig(val port: Int,val totalClients: Int, val paths: List<String>)
data class WsClientCommConfigsV1(val msgCount:Int, val clientConfigs: MutableList<WsClientConfig>)
data class WsClientCommConfigsV2(val msgCount:Int, val broadcastPorts: MutableList<Int>, val clientConfigs: MutableList<WsClientConfig>)
data class WebSocketServerStartup(val portsToWebSocketPaths: MutableMap<Int, MutableList<String>>)

open class TestServerReceiver<T>(receive: (SocketAddress, T) -> Unit) : MessageReceiver<T>(receive) {
    var messageCount = 0
    var messageList = mutableListOf<T>()
}

open class TestClientReceiver<T>(val client: Client<T>, receive: (SocketAddress, T) -> Unit) : MessageReceiver<T>(receive) {
    var messageCount = 0
    var messageList = mutableListOf<T>()
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

fun <T> areClientsActive(clientList: MutableList<Client<T>>):Boolean{
    if(clientList.isEmpty()) return false
    var allActive = true
    clientList.forEach { client ->
        if(!client.isActive()){
            allActive = false
        }

    }
    return allActive
}

fun <T> areClientsInactive(clientList: MutableList<Client<T>>):Boolean{
    if(clientList.isEmpty()) return true
    var allInactive = true
    clientList.forEach { client ->
        if(client.isActive()){
            allInactive = false
        }
    }
    return allInactive
}

@ExperimentalTime
suspend fun <T> startServer(server: Server<T>) : Boolean{
    server.startServer()
    until(activeTime.seconds) {
        server.allActive()
    }
    return true
}

@ExperimentalTime
suspend fun <T> connectClients(clientList : MutableList<Client<T>>) : Boolean{
    clientList.forEach { client ->
        client.connect()
        Thread.sleep(10)
    }
    until(activeTime.seconds) {
        areClientsActive(clientList)
    }
    return true
}

@ExperimentalTime
suspend fun <T> disconnectClients(clientList : MutableList<Client<T>>) : Boolean{
    clientList.forEach { client ->
        client.disconnect()
        Thread.sleep(10)
    }
    until(activeTime.seconds) {
        areClientsInactive(clientList)
    }
    return true
}

@ExperimentalTime
suspend fun <T> waitForMessagesServer(totalMessages:Int ,receiverList : MutableList<out TestServerReceiver<T>>,debug: Boolean = false) : Boolean{
    var waitCount = 1
    until(waitForMsgTime.seconds) {
        val received: Int = receiverList.sumOf { it.messageCount }
        if(debug){
            println("Server received: $received out of $totalMessages (check ${waitCount++})")
        }
        received == totalMessages
    }
    return true
}

@ExperimentalTime
suspend fun <T> waitForMessagesClient(totalMessages:Int ,receiverList : MutableList<out TestClientReceiver<T>>,debug: Boolean = false) : Boolean{
    var waitCount = 1
    until(waitForMsgTime.seconds) {
        val received: Int = receiverList.sumOf { it.messageCount }
        if(debug){
            println("Clients received: $received out of $totalMessages (check ${waitCount++})")
        }
        received == totalMessages
    }
    return true
}

@ExperimentalTime
suspend fun waitForClientStatusChange(totalChanges:Int, list : MutableList<out StatusListener>, debug: Boolean = false, checkConnect:Boolean = false) : Boolean{
    until(waitForMsgTime.seconds) {
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
    return true
}

@ExperimentalTime
suspend fun waitForServerStatusChange(totalChanges:Int, list : MutableList<out StatusListener>, debug: Boolean = false, checkConnect:Boolean = false) : Boolean{
    until(waitForMsgTime.seconds) {
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
    return true
}
