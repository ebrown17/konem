package eb

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main(){
    logger.info("hello from main")
    onConnection()
    logger.info("after")
    runBlocking { delay(5000) }
    logger.info("After delay")

}

fun onConnection() {
    logger.info("On COnnection called")

    GlobalScope.launch {
            logger.info(" in launch")
            withTimeout(1500L) {
                logger.info(" withTimeout ")
                delay(1000)
                for(x  in 1..10){
                    logger.info("loop: {}",x.toInt())
                }
            }
        }

}