package konem.protocol.websocket

import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konem.data.json.KonemMesssage
import konem.data.json.KonemMessageAdaptor
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*

class KonemMessageReceiver(private val receive: (InetSocketAddress, KonemMesssage) -> Unit) :
  ReceiverHandler<String>() {
  private val logger = LoggerFactory.getLogger(KonemMessageReceiver::class.java)
  private val adaptor = moshi.adapter(KonemMesssage::class.java)

  override fun read(addr: InetSocketAddress, message: String) {
    try {
      val kMessage = adaptor.fromJson(message)
      if (kMessage != null) {
        receive(addr, kMessage)
      }
    } catch (e: Exception) {
      logger.error("exception in casting of message : {} ", e.message)
    }
  }

  companion object {
    val moshi: Moshi =
      Moshi.Builder().add(KonemMessageAdaptor()).add(KotlinJsonAdapterFactory()).build()
  }
}
