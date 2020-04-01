package konem.protocol.socket.json

import java.net.SocketAddress
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.ReceiverHandler
import kotlinx.serialization.json.JsonDecodingException
import org.slf4j.LoggerFactory

open class KonemMessageReceiver(private val receive: (SocketAddress, KonemMessage) -> Unit) :
  ReceiverHandler<KonemMessage>() {
  private val logger = LoggerFactory.getLogger(KonemMessageReceiver::class.java)

  // TODO look at using channels to pass value from receiver

  override fun read(addr: SocketAddress, message: KonemMessage) {
    synchronized(this) {
      try {
        receive(addr, message)
      } catch (e: JsonDecodingException) {
        logger.error("JsonParsingException in serializing to KonemMessage with message: {} ", e.message)
      }
    }
  }
}
