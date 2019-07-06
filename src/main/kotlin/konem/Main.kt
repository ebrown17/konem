package konem

import com.squareup.moshi.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konem.protocol.websocket.KoneMessageReceiver
import konem.protocol.websocket.KoneMesssage
import java.util.*


private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)


fun main() {
    logger.info("hello from main")

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val adapter = moshi.adapter(KoneMesssage::class.java)

    var receiver = KoneMessageReceiver { remote, message ->
        logger.info("KoneMessageReceiver: {} ", message)
    }

    val testo = KoneMesssage("heartbeat", Date().toString())
    println(testo)
    var jjson = adapter.toJson(testo)

    logger.info(" jjson dmsg: {}", testo)
    logger.info(" jjson string: {}", jjson)
    receiver.handleChannelRead(InetSocketAddress(8080), jjson)

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
