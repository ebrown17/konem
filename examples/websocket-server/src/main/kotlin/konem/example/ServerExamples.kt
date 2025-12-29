package konem.example

import konem.Konem
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.ConnectionListener
import konem.netty.ConnectionStatusListener
import konem.netty.MessageReceiver
import konem.netty.ServerHeartbeatProtocol
import konem.netty.stream.DisconnectionListener
import konem.protocol.konem.KonemProtocolPipeline
import konem.protocol.websocket.json.KonemMessageReceiver
import konem.protocol.websocket.json.WebSocketClientFactory
import konem.protocol.websocket.json.WebSocketConnectionStatusListener
import konem.protocol.websocket.json.WebSocketServer
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.net.SocketAddress


private val logger = LoggerFactory.getLogger("Main")


fun main(){
  websocketServerExamples()
}

fun websocketServerExamples() {

  //val server = WebSocketServer()
  //server.addChannel(8080, "/tester")

    val server = Konem.createWebSocketServer(
        config = {
            it.addChannel(8080,"/tester")
            it.USE_SSL = false
        },
        heartbeatProtocol = ServerHeartbeatProtocol{ KonemMessage(Heartbeat()) },
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )



  server.startServer()
  var count = 0

    sleep(5000)


  server.registerChannelMessageReceiver(MessageReceiver<KonemMessage> { _, message ->
      logger.info("KoneMessageReceiver: got {} ", message)
      count++
  })

  server.registerConnectionStatusListener(
      ConnectionStatusListener(
          connected = { remoteAddr  ->
              logger.info("Connection from {} to path {}", remoteAddr )
          }, disconnected = { remoteAddr ->
              logger.info("Disconnection from  {} to path {}", remoteAddr)
          }
      ))

  val fact = WebSocketClientFactory()
  val client = fact.createClient("localhost", 8080, "/tester")
  val client2 = fact.createClient("localhost", 8080, "/tester")
  val client3 = fact.createClient("localhost", 8080, "/tester")
  val connectionListener = ConnectionListener { remoteAddr: SocketAddress->
    logger.info("Client connected to {}", remoteAddr)
  }

 // client.registerConnectionListener(connectionListener)

  client.registerDisconnectionListener(DisconnectionListener { remoteAddr ->
    logger.info("Client {} disconnected from {}", client.toString(), remoteAddr)
  })

  client.connect()
  client2.connect()
  client3.connect()

  Thread.sleep(1000)

  repeat(10) {
    client.sendMessage(KonemMessage(Heartbeat("$it")))
    Thread.sleep(500)
  }
  Thread.sleep(1000)
  println(count)
  count = 0

  client.disconnect()
  client2.disconnect()
  client3.disconnect()

  sleep(5000)
    println(count)
  server.shutdownServer()
}
