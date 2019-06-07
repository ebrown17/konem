package eb

import com.squareup.moshi.*
import eb.protocol.websocket.JsonMessageReceiver
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory


private val logger = LoggerFactory.getLogger("Main")
private val cName = CoroutineName("onConnection")
private val scopey = CoroutineScope(cName)

@JsonClass(generateAdapter = true)
data class DMesssage(val msg_1: Map<String, Any>)

fun main() {
    logger.info("hello from main")

    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val adapter= moshi.adapter(DMesssage::class.java)

    var receiver = JsonMessageReceiver { remote, msg ->
           logger.info("READEERRRR {} {}",remote,msg)
            val bb = "$msg{hhh :"
            val dmsg:DMesssage? = adapter.fromJson(msg)
        if (dmsg != null) {
            val tty = dmsg.msg_1["1"]
            logger.info("dmg: {} type of 87:{}", dmsg,tty!!::class.java.name)
        }

    }


    val testo = DMesssage(mapOf("1" to 87, "2" to listOf<Int>(1,2,3,4,5,6,7)))
    var jjson = adapter.toJson(testo)

    logger.info(" jjson dmsg: {}",testo)
    logger.info(" jjson string: {}",testo)
   // val tester =JsonReader.
    receiver.handleChannelRead(InetSocketAddress(8080),jjson)

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