package eb.netty.stream

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

abstract class ReceiverHandler<I> : Receiver<I> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun handleChannelRead(addr: InetSocketAddress, msg: I) {
        try {
            read(addr, msg)
        } catch (e: Exception) {
            logger.error("handleReadMessage exception in casting of message : {} ", e.message)
        }

    }

    /**
     * If reads from multiple sources will be read, this method should be synchronized
     *
     * @param addr address from where message originated
     * @param message
     */
    abstract fun read(addr: InetSocketAddress, message: I)


}