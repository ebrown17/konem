package konem.protocol.socket.json

import java.net.SocketAddress
import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.ReceiverHandler
import kotlinx.serialization.json.JsonDecodingException
import org.slf4j.LoggerFactory

open class KonemMessageReceiver(private val receive: (SocketAddress, KonemMessage) -> Unit) :
  ReceiverHandler<String>() {
  private val logger = LoggerFactory.getLogger(KonemMessageReceiver::class.java)
  private val serializer = KonemMessageSerializer()

  // TODO look at using channels to pass value from receiver

  override fun read(addr: SocketAddress, message: String) {
    synchronized(this) {
      try {
        val kMessage = serializer.toKonemMessage(message)
        receive(addr, kMessage)
      } catch (e: JsonDecodingException) {
        logger.error("JsonParsingException in serializing to KonemMessage with message: {} ", e.message)
      }
    }
  }
}
