package konem

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.protocol.websocket.KonemMessageReceiver
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)

fun main() {
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

  val kotlinStatus = KonemMessage(Message.Status("Good Times",0,500,199,"All good here"))
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

  var receiver = KonemMessageReceiver { remote, message ->
    logger.info("KoneMessageReceiver: {} ", message)
  }
  receiver.handleChannelRead(InetSocketAddress(8080),jsonBeat)
  receiver.handleChannelRead(InetSocketAddress(8080),jsonStatus)
  receiver.handleChannelRead(InetSocketAddress(8080),jsonUnknown)
Thread.sleep(1000)
  logger.info("{}",KonemMessage(Message.Heartbeat()))

  Thread.sleep(1000)
  logger.info("{}",KonemMessage(Message.Status()))

  Thread.sleep(1000)
  logger.info("{}",KonemMessage(Message.Heartbeat()))
  logger.info("{}",KonemMessage(Message.Unknown()))

}
