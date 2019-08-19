package konem.protocol.wire

import konem.data.protobuf.KonemMessage
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

open class KonemPMessageReceiver(private val receive: (InetSocketAddress, KonemMessage) -> Unit) :
  ReceiverHandler<Any>() {
  private val logger = LoggerFactory.getLogger(KonemPMessageReceiver::class.java)

  // TODO look at using channels to pass value from receiver
  override fun read(addr: InetSocketAddress, message: Any) {
    synchronized(this) {
        when (message) {
          is KonemMessage -> receive(addr, message)
          else -> {
            logger.error("read got unexpected message type: {} ", message.javaClass)
          }
        }
    }
  }
}
