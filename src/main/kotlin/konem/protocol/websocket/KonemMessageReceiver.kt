package konem.protocol.websocket

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.ReceiverHandler
import kotlinx.serialization.json.JsonParsingException
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

open class KonemMessageReceiver(private val receive: (InetSocketAddress, KonemMessage) -> Unit) :
  ReceiverHandler<String>() {
  private val logger = LoggerFactory.getLogger(KonemMessageReceiver::class.java)
  private val serializer = KonemMessageSerializer()

  //TODO look at using channels to pass value from receiver

  override fun read(addr: InetSocketAddress, message: String) {
    synchronized(this) {
      try {
        val kMessage = serializer.toKonemMessage(message)
        receive(addr, kMessage)
      } catch (e: JsonParsingException) {
        logger.error("JsonParsingException in serializing to KonemMessage with message: {} ", e.message)
      }
    }
  }
}
