package konem.example

import konem.data.json.KonemMessage
import konem.data.json.Message
import konem.netty.stream.ConnectionListener
import konem.netty.stream.ConnectionStatusListener
import konem.netty.stream.DisconnectionListener
import konem.protocol.websocket.json.KonemMessageReceiver
import konem.protocol.websocket.json.WebSocketClientFactory
import konem.protocol.websocket.json.WebSocketServer
import org.slf4j.LoggerFactory


private val logger = LoggerFactory.getLogger("Main")


fun main(){
  websocketServerExamples()
}

fun websocketServerExamples() {

  val server = WebSocketServer()
  server.addChannel(8080, "/tester")
  server.startServer()
  var count = 0

  server.registerChannelReadListener(KonemMessageReceiver { _, message ->
    logger.info("KoneMessageReceiver: {} ", message)
    count++
  })

  server.registerConnectionStatusListener(
    ConnectionStatusListener(
    connected = { remoteAddr ->
      logger.info("Connection from {}", remoteAddr)
    }, disconnected = { remoteAddr ->
      logger.info("Disconnection from {}", remoteAddr)
    }
  ))

  val fact = WebSocketClientFactory()
  val client = fact.createClient("localhost", 8080, "/tester")
  val client2 = fact.createClient("localhost", 8080, "/tester")

  val connectionListener = ConnectionListener { remoteAddr ->
    logger.info("Client connected to {}", remoteAddr)
  }

  client.registerConnectionListener(connectionListener)

  client.registerDisconnectionListener(DisconnectionListener { remoteAddr ->
    logger.info("Client {} disconnected from {}", client.toString(), remoteAddr)
  })

  client2.registerConnectionListener(connectionListener)

  client2.registerDisconnectionListener(DisconnectionListener { remoteAddr ->
    logger.info("Client {} disconnected from {}", client.toString(), remoteAddr)
  })

  client.connect()

  client2.connect()

  Thread.sleep(1000)

  repeat(10) {
    client.sendMessage(KonemMessage(Message.Heartbeat("$it")))
    client2.sendMessage(KonemMessage(Message.Heartbeat("$it")))
    Thread.sleep(1000)
  }
  Thread.sleep(1000)
  println(count)
  count = 0

  client.disconnect()
  client2.disconnect()
  server.shutdownServer()
}
