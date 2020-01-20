package konem.netty.stream

import java.lang.ClassCastException
import java.net.SocketAddress
import org.slf4j.LoggerFactory

@Suppress("UNCHECKED_CAST")
abstract class ReceiverHandler<I> : Receiver {

  private val logger = LoggerFactory.getLogger(javaClass)

  override fun handle(addr: SocketAddress, msg: Any) {
    try {
      read(addr, msg as I)
    } catch (e: ClassCastException) {
      logger.error("exception in casting of message : {}", e.message)
    }
  }

  /**
   * If reads from multiple sources will be read, this method should be synchronized
   *
   * @param addr address from where message originated
   * @param message
   */
  abstract fun read(addr: SocketAddress, message: I)
}
