package konem

import com.squareup.moshi.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konem.data.json.KonemMessageAdaptor
import konem.data.json.KonemMesssage
import konem.protocol.websocket.*

private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)

fun main() {
  logger.info("hello from main")

  val moshi =
    Moshi.Builder().add(KonemMessageAdaptor()).add(KotlinJsonAdapterFactory()).build()

  val adapter = moshi.adapter(KonemMesssage::class.java)

  var receiver = KonemMessageReceiver { remote, message ->
    logger.info("KoneMessageReceiver: {} ", message as KonemMesssage)
  }

  val heartb = KonemMesssage.Heartbeat()
  var jheartb = adapter.toJson(heartb)
  logger.info("ACTUAL: {}", heartb)
  logger.info("JSON string: {}", jheartb)
  val heartBack = adapter.fromJson(jheartb)
  logger.info("converted back to ACTUAL: {}", heartBack)
  receiver.handleChannelRead(InetSocketAddress(8080), jheartb)

  val statusb = KonemMesssage.Status("GOOD", 0, 5000, 0, "GOOD")
  var jstatusb = adapter.toJson(statusb)
  logger.info("statusb before json: {}", statusb)
  logger.info("statusb json string: {}", jstatusb)
  var statusBack: KonemMesssage? = null
  println(jstatusb)
  statusBack = adapter.fromJson(jstatusb)
  logger.info("statusb converted back to heartbxxx: {}", statusBack)
  receiver.handleChannelRead(InetSocketAddress(8080), jstatusb)
}
