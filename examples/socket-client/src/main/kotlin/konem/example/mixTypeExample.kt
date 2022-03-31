package konem.example


import konem.netty.ConnectionListener

import konem.protocol.string.StringClientFactory
import konem.protocol.string.StringMessageReceiver
import konem.protocol.string.StringServer
import java.util.*


class Tester<I>(private val generateHeartBeats: () -> I) {
    fun generateHeartBeat(): I  {
        return generateHeartBeats()
    }
}

fun main() {

    val heartbeatProducer = Tester {
        "SDFSDFSDF ${System.currentTimeMillis()}"
    }

    println(heartbeatProducer.generateHeartBeat())
    Thread.sleep(200)
    println(heartbeatProducer.generateHeartBeat())



}
