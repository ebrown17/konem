package konem.example

import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import konem.Konem
import konem.data.json.Heartbeat
import konem.data.json.KonemMessage
import konem.netty.MessageReceiver
import konem.netty.ServerHeartbeatProtocol
import konem.netty.stream.ConnectionListener
import konem.netty.stream.DisconnectionListener
import konem.netty.stream.Receiver
import konem.protocol.konem.KonemProtocolPipeline
import konem.protocol.websocket.WebSocketConnectionStatusListener
import konem.protocol.websocket.json.KonemMessageReceiver
import konem.protocol.websocket.json.WebSocketClientFactory
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
        },
        heartbeatProtocol = ServerHeartbeatProtocol(true,10) { PingWebSocketFrame() },
        protocolPipeline = KonemProtocolPipeline.getKonemJsonPipeline()
    )

  server.startServer()
  var count = 0

  sleep(5000)

  server.registerChannelMessageReceiver(MessageReceiver { from, message ->
      logger.info("Server KoneMessageReceiver: got {} from {} ", message,from)
      sleep(3000)
      server.broadcastOnAllChannels(KonemMessage(Heartbeat("${count++}")))
  },"/tester")

  server.registerPathConnectionStatusListener(
      WebSocketConnectionStatusListener(
          connected = { remoteAddr, wsPath ->
              logger.info("Connection from {} to path {}", remoteAddr, wsPath)
          }, disconnected = { remoteAddr, wsPath ->
              logger.info("Disconnection from  {} to path {}", remoteAddr, wsPath)
          }
      ))



  val fact = WebSocketClientFactory()
  val client = fact.createClient("localhost", 8080, "/tester")
  val connectionListener = ConnectionListener { remoteAddr: SocketAddress ->
      logger.info("Client connected to {}", remoteAddr)
      sleep(2000)
    //  client.sendMessage(KonemMessage(Heartbeat("${count++}")))
  }

  client.registerConnectionListener(connectionListener)

  client.registerDisconnectionListener(DisconnectionListener { remoteAddr ->
    logger.info("Client {} disconnected from {}", client.toString(), remoteAddr)
  })

  //client.connect()

  client.registerChannelReadListener(KonemMessageReceiver { from, msg ->
      logger.info("Client KonemMessageReceiver: got {} from {}", from, msg)
      sleep(3000)
      client.sendMessage(KonemMessage(Heartbeat("${count++}")))
  })


sleep(60_000)

    client.disconnect()

  sleep(5000)
    println(count)
  server.shutdownServer()
}
