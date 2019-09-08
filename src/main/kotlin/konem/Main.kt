package konem

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.netty.stream.ConnectionListener
import konem.netty.stream.ConnectionStatusListener
import konem.netty.stream.DisconnectionListener
import konem.protocol.websocket.KonemMessageReceiver
import konem.protocol.websocket.WebSocketClientFactory
import konem.protocol.websocket.WebSocketServer
import konem.protocol.wire.WireClientFactory
import konem.protocol.wire.WireMessageReceiver
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)

@Suppress("MagicNumber")
fun main2() {

  val server = WebSocketServer()
  server.addChannel(8080, "/tester")
  server.startServer()
  var count = 0

  server.registerChannelReadListener(KonemMessageReceiver { _, message ->
    logger.info("KoneMessageReceiver: {} ", message)
    count++
  })

  server.registerConnectionStatusListener(
    ConnectionStatusListener(connected = { remoteAddr ->
      logger.info("Connection from {}", remoteAddr)
    }, disconnected = { remoteAddr ->
      logger.info("Disconnection from {}", remoteAddr)
    })
  )

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
    // Thread.sleep(1000)
  }
  Thread.sleep(1000)
  println(count)
  count = 0

  client.disconnect()
  client2.disconnect()
}

@Suppress("MagicNumber")
fun main1() {
  logger.info("hello from main")
  val serializer = KonemMessageSerializer()
  var count = 0

  for (i in 1..1000) {
    for (j in 1..1000) {
      val kotlinBeat = KonemMessage(Message.Heartbeat())
      val jsonBeat = serializer.toJson(kotlinBeat)
      serializer.toKonemMessage(jsonBeat)
    }
  }

  val timeT = measureTimeMillis {
    for (i in 1..1000) {
      for (j in 1..100) {
        val kotlinBeat = KonemMessage(Message.Heartbeat())
        val jsonBeat = serializer.toJson(kotlinBeat)
        serializer.toKonemMessage(jsonBeat)
        count++
      }
    }
  }

  println("Kotlin $count Took $timeT ms")

  val kotlinBeat = KonemMessage(Message.Heartbeat())
  val jsonBeat = serializer.toJson(kotlinBeat)
  val back = serializer.toKonemMessage(jsonBeat)

  println(kotlinBeat)
  println(jsonBeat)
  println(back)

  val kotlinStatus = KonemMessage(Message.Status("Good Times", 0, 500, 199, "All good here"))
  val jsonStatus = serializer.toJson(kotlinStatus)
  val statBack = serializer.toKonemMessage(jsonStatus)

  println(kotlinStatus)
  println(jsonStatus)
  println(statBack)

  val kotlinUnknown = KonemMessage(Message.Unknown())
  val jsonUnknown = serializer.toJson(kotlinUnknown)
  val unknownBack = serializer.toKonemMessage(jsonUnknown)

  println(kotlinUnknown)
  println(jsonUnknown)
  println(unknownBack)

  var receiver = KonemMessageReceiver { _, message ->
    logger.info("KoneMessageReceiver: {} ", message)
  }
  receiver.receive(InetSocketAddress(8080), jsonBeat)
  receiver.receive(InetSocketAddress(8080), jsonStatus)
  receiver.receive(InetSocketAddress(8080), jsonUnknown)
  sleep(1000)
  logger.info("{}", KonemMessage(Message.Heartbeat()))

  sleep(1000)
  logger.info("{}", KonemMessage(Message.Status()))

  sleep(1000)
  logger.info("{}", KonemMessage(Message.Heartbeat()))
  logger.info("{}", KonemMessage(Message.Unknown()))
}

fun main() {
  val server = konem.protocol.wire.WireServer()
  server.addChannel(8085)
  server.startServer()

  server.registerChannelReadListener(WireMessageReceiver { inetSocketAddress, konemMessage ->
    logger.info("xxxx KoneMessageReceiver: {} ", konemMessage.toString())
  })

  val client = WireClientFactory().createClient("localhost", 8085)
  client.connect()

  client.registerConnectionListener(ConnectionListener {
    println("CLuient connected")
    client.sendMessage(wireMessage("Client connected to $it"))
    repeat(10) { time ->
      client.sendMessage(wireMessage("Client send $time"))
      sleep(100)
    }
  })
}

fun wireMessage(test: String): konem.data.protobuf.KonemMessage {
  return konem.data.protobuf.KonemMessage(
    messageType = konem.data.protobuf.KonemMessage.MessageType.DATA,
    data = konem.data.protobuf.KonemMessage.Data(test)
  )
}
