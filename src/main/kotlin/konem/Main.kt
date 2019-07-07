package konem

import com.squareup.moshi.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konem.protocol.websocket.*
import java.util.*
import kotlin.system.measureTimeMillis


private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)


fun main() {
  logger.info("hello from main")

  val moshi =
    Moshi.Builder().add(KonemMesssageAdaptor()).add(KotlinJsonAdapterFactory()).build()

  val adapter = moshi.adapter(KonemMesssage::class.java)

  var receiver = KoneMessageReceiver { remote, message ->
    logger.info("KoneMessageReceiver: {} ", message as KonemMesssage)
  }

  val heartb = KonemMesssage.Heartbeat()
  var jheartb = adapter.toJson(heartb)
  logger.info("ACTUAL: {}", heartb)
  logger.info("JSON string: {}", jheartb)
  val heartBack = adapter.fromJson(jheartb)
  logger.info("converted back to ACTUAL: {}", heartBack)
  receiver.handleChannelRead(InetSocketAddress(8080), jheartb)
/*
  val statusb = KonemMesssage.Status("GOOD",0,5000,0,"GOOD")
  var jstatusb = adapter.toJson(statusb)
  logger.info("statusb before json: {}", statusb)
  logger.info("statusb json string: {}", jstatusb)
  var statusBack : KonemMesssage? = null
  statusBack = adapter.fromJson(jstatusb)
  logger.info("statusb converted back to heartbxxx: {}", statusBack)
*/



 // receiver.handleChannelRead(InetSocketAddress(8080), jstatusb)

/*    runBlocking { tester() }*/
  // onConnection()
  //val job =tester2()
  logger.info("after")
  //  logger.info("active: {} cancel: {} completed: {}",job.isActive,job.isCancelled,job.isCompleted)
  runBlocking { delay(5000) }
  logger.info("After delay")
  // logger.info("active: {} cancel: {} completed: {}",job.isActive,job.isCancelled,job.isCompleted)
}

fun onConnection() {
  logger.info("On COnnection called")
  scopey.launch {
    logger.info(" in launch.. delay")
    delay(1000)
    logger.info(" in launch.. after ")
    for (x in 1..10) {
      launch {
        withTimeout(1500L) {
          logger.info(" in withTImeout ")
          logger.info("loop: {}", this.coroutineContext.toString())
        }
      }
    }
  }
}

suspend fun tester() {
  logger.info("Tester 2 called")
  coroutineScope {
    logger.info(" in launch")
    withTimeout(1500L) {
      logger.info(" withTimeout ")
      delay(1000)
      for (x in 1..10) {
        logger.info("loop: {}", this.coroutineContext.toString())
      }
    }
  }
}

fun tester2() {
  logger.info("On COnnection called")

  scopey.launch {
    logger.info(" in launch.. delay")
    delay(1000)
    logger.info(" in launch.. after ")
    for (x in 1..10) {
      launch {
        withTimeout(1500L) {
          logger.info(" in withTImeout ")
          logger.info("loop: {}", this.coroutineContext.toString())
        }
      }
    }
  }
}
