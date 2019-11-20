package examples.data.json

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.protocol.websocket.KonemMessageReceiver
import kotlinx.coroutines.CoroutineName
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.lang.Thread.sleep

private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")

fun main(){
  konemJsonMessageSerializer()
}

@Suppress("MagicNumber")
fun konemJsonMessageSerializer() {
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

  logger.info("Kotlin serialization of $count Took $timeT ms")

  val kotlinBeat = KonemMessage(Message.Heartbeat())
  val jsonBeat = serializer.toJson(kotlinBeat)
  val back = serializer.toKonemMessage(jsonBeat)

  logger.info("kotlinBeat {}",kotlinBeat)
  logger.info("jsonBeat {}",jsonBeat)
  logger.info("back {}",back)

  val kotlinStatus = KonemMessage(Message.Status("Good Times", 0, 500, 199, "All good here"))
  val jsonStatus = serializer.toJson(kotlinStatus)
  val statBack = serializer.toKonemMessage(jsonStatus)

  logger.info("kotlinStatus {}",kotlinStatus)
  logger.info("jsonStatus {}",jsonStatus)
  logger.info("statBack {}",statBack)

  val kotlinUnknown = KonemMessage(Message.Unknown())
  val jsonUnknown = serializer.toJson(kotlinUnknown)
  val unknownBack = serializer.toKonemMessage(jsonUnknown)

  logger.info("kotlinUnknown {}",kotlinUnknown)
  logger.info("jsonUnknown {}",jsonUnknown)
  logger.info("unknownBack {}",unknownBack)

  var receiver = KonemMessageReceiver { _, message ->
    logger.info("KoneMessageReceiver: {} ", message)
  }
  receiver.handle(InetSocketAddress(8080), jsonBeat)
  receiver.handle(InetSocketAddress(8080), jsonStatus)
  receiver.handle(InetSocketAddress(8080), jsonUnknown)
  sleep(1000)
  logger.info("{}", KonemMessage(Message.Heartbeat()))

  sleep(1000)
  logger.info("{}", KonemMessage(Message.Status()))

  sleep(1000)
  logger.info("{}", KonemMessage(Message.Heartbeat()))
  logger.info("{}", KonemMessage(Message.Unknown()))
}
