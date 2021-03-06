package konem.example

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.data.json.Message
import konem.protocol.websocket.json.KonemMessageReceiver
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis
import java.lang.Thread.sleep


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

  println("Kotlin serialization of $count Took $timeT ms")

  val kotlinBeat = KonemMessage(Message.Heartbeat())
  val jsonBeat = serializer.toJson(kotlinBeat)
  val back = serializer.toKonemMessage(jsonBeat)

  println("kotlinBeat $kotlinBeat")
  println("jsonBeat $jsonBeat")
  println("back $back")

  val kotlinStatus = KonemMessage(Message.Status("Good Times", 0, 500, 199, "All good here"))
  val jsonStatus = serializer.toJson(kotlinStatus)
  val statBack = serializer.toKonemMessage(jsonStatus)

  println("kotlinStatus $kotlinStatus")
  println("jsonStatus $jsonStatus")
  println("statBack $statBack")

  val kotlinUnknown = KonemMessage(Message.Unknown())
  val jsonUnknown = serializer.toJson(kotlinUnknown)
  val unknownBack = serializer.toKonemMessage(jsonUnknown)

  println("kotlinUnknown $kotlinUnknown")
  println("jsonUnknown $jsonUnknown")
  println("unknownBack $unknownBack")


  sleep(1000)
  println("${KonemMessage(Message.Heartbeat())}")

  sleep(1000)
  println("${KonemMessage(Message.Status())}")

  sleep(1000)
  println("${KonemMessage(Message.Heartbeat())}")
  println("${KonemMessage(Message.Unknown())}")
}
