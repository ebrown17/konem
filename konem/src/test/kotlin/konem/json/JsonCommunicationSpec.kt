package konem.json

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.AfterEach
import io.kotest.core.spec.BeforeEach
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import konem.data.json.Data
import konem.data.json.KonemMessage
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import konem.logger
import konem.protocol.socket.json.JsonClient
import konem.protocol.socket.json.JsonClientFactory
import konem.protocol.socket.json.JsonServer
import konem.protocol.socket.json.KonemMessageReceiver
import java.net.SocketAddress

lateinit var server: JsonServer
lateinit var clientFactory:  JsonClientFactory

var testSetup = {
    server = JsonServer()
    server.addChannel(6060)
    server.addChannel(6061)
    server.addChannel(6062)
    server.addChannel(6063)

    clientFactory = JsonClientFactory()
}

var testCleanUp = {
    clientFactory.shutdown()
    server.shutdownServer()

}

class JsonTestReceiver(receive: (SocketAddress, KonemMessage) -> Unit) : KonemMessageReceiver(receive) {
    var messageCount = 0
    var messageList = mutableListOf<String>()
    var clientId = ""
}

data class ClientConfig(val port: Int,val totalClients: Int)

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

@ExperimentalTime
@ExperimentalKotest
class JsonCommunicationSpec : ShouldSpec( {
      should("Server readers can register before server starts and then see messages") {
          forAll(
              row(1, arrayOf(ClientConfig(6060,1))),
              row(5, arrayOf(ClientConfig(6060,10))),
              row(5, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
              row(35, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
              row(49, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                  ClientConfig(6062,21))),
              row(66, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                  ClientConfig(6062,21), ClientConfig(6063,43))),
              ) { sends, configs ->
              testSetup()
              lateinit var serverReceiver : JsonTestReceiver
              var totalMessages = 0
              val clientList = mutableListOf<JsonClient>()

              serverReceiver = JsonTestReceiver { from, msg ->
                  serverReceiver.messageCount++
              }

              configs.forEach { config  ->
                  totalMessages += (config.totalClients * (sends))
                  server.registerChannelReadListener(config.port, serverReceiver)
                  for(i in 1..config.totalClients){
                      clientList.add(clientFactory.createClient("localhost",config.port))
                  }
              }

              server.startServer()

              until(Duration.seconds(3), Duration.milliseconds(250).fixed()) {
                  server.allActive()
              }

              clientList.forEach { client -> client.connect() }

              until(Duration.seconds(5), Duration.milliseconds(250).fixed()) {
                  areClientsActive(clientList)
              }

              clientList.forEachIndexed { index, client ->
                  for(i in 1..sends){
                      client.sendMessage(KonemMessage(message = Data("Client $index message $i") ))
                  }
              }

              until(Duration.seconds(5), Duration.milliseconds(250).fixed()) {
                  println("${serverReceiver.messageCount} == $totalMessages")
                  serverReceiver.messageCount == totalMessages
              }
              println("-----------------------------------")
              testCleanUp()
          }
      }
    should("Server readers can register after server starts and then see messages") {
        forAll(
            row(1, arrayOf(ClientConfig(6060,1))),
            row(5, arrayOf(ClientConfig(6060,10))),
            row(5, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(35, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10))),
            row(49, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21))),
            row(66, arrayOf(ClientConfig(6060,1), ClientConfig(6061,10),
                ClientConfig(6062,21), ClientConfig(6063,43))),
        ) { sends, configs ->
            testSetup()
            lateinit var serverReceiver : JsonTestReceiver
            var totalMessages = 0
            val clientList = mutableListOf<JsonClient>()

            serverReceiver = JsonTestReceiver { from, msg ->
                serverReceiver.messageCount++
            }

            server.startServer()

            configs.forEach { config  ->
                totalMessages += (config.totalClients * (sends))
                server.registerChannelReadListener(config.port, serverReceiver)

                for(i in 1..config.totalClients){
                    clientList.add(clientFactory.createClient("localhost",config.port))
                }
            }
            until(Duration.seconds(3), Duration.milliseconds(250).fixed()) {
                server.allActive()
            }

            clientList.forEach { client -> client.connect() }

            until(Duration.seconds(5), Duration.milliseconds(250).fixed()) {
                areClientsActive(clientList)
            }

            clientList.forEachIndexed { index, client ->
                for(i in 1..sends){
                    client.sendMessage(KonemMessage(message = Data("Client $index message $i") ))
                }
            }

            until(Duration.seconds(5), Duration.milliseconds(250).fixed()) {
                println("${serverReceiver.messageCount} == $totalMessages")
                serverReceiver.messageCount == totalMessages
            }
            println("-----------------------------------")
            testCleanUp()
        }
    }



})

