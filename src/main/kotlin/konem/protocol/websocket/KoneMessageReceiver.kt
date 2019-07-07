package konem.protocol.websocket


import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*


@JsonClass(generateAdapter = true)
data class KoneMesssage(val type: String) {

  constructor(type: String, message: Any) : this(type)

  companion object {
    private const val heartbeat = "heartbeat"
    fun KoneHeartbeat(): KoneMesssage {
      return KoneMesssage(heartbeat, Date().toString())
    }
  }
}

enum class KonemTypes {
  @Json(name = "heartbeat")
  HEARTBEAT,
  @Json(name = "status")
  STATUS,
  @Json(name = "unknown")
  UNKNOWN
}

class MainMessageAdaptor {

  val typeOption = JsonReader.Options.of("type","heartbeat","status","unknown")
  val heartOpt = JsonReader.Options.of("message","sent")

  @FromJson
  fun fromJson(jsonReader: JsonReader): MainMessage {

    jsonReader.beginObject()
    if(jsonReader.selectName(typeOption) == 0){
      val type = jsonReader.selectString(typeOption)
      if(type == 1){
        val ttt = jsonReader.selectName(heartOpt)
        if(ttt == 0 ){
          jsonReader.beginObject()
          jsonReader.selectName(heartOpt)
          val msg = jsonReader.nextString()

          jsonReader.endObject()
          jsonReader.endObject()
          return   MainMessage(KonemTypes.HEARTBEAT, KonemHeartbeat(msg))
        }
      }

    }


    return MainMessage(KonemTypes.UNKNOWN, KonemTypes.UNKNOWN.toString())
  }


  private fun extract(name: String, reader: JsonReader): String {
    var extracted = ""
    while (reader.hasNext() &&(reader.peek() != JsonReader.Token.END_DOCUMENT) ) {

      if (reader.peek() == JsonReader.Token.NAME) {
        val check = reader.nextName()
        if (check == name) {
          extracted = reader.nextString()
        }
        else{
          reader.skipValue()
        }
      }
      println("XXX $reader EXTRACTED: [ $extracted ]")

    }

    return extracted
  }

  @ToJson
  fun toJson(message: MainMessage) = when (message.type) {
    KonemTypes.HEARTBEAT -> {
      message
    }
    KonemTypes.STATUS -> {
      message
    }
    else -> {
      MainMessage(KonemTypes.UNKNOWN, message.message)
    }
  }

  companion object {
    const val heartbeat: String = "heartbeat"
  }
}


//@JsonClass(generateAdapter = true)
data class MainMessage(val type: KonemTypes, val message: Any) {

  companion object {
    fun Heartbeat(): MainMessage {
      return MainMessage(KonemTypes.HEARTBEAT, KonemHeartbeat(Date().toString()))
    }

    fun Status(
      shortName: String,
      errors: Int,
      received: Int,
      sent: Int,
      description: String
    ): MainMessage {
      return MainMessage(
        KonemTypes.STATUS,
        KonemStatus(shortName, errors, received, sent, description)
      )
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemHeartbeat(val sent: String)

@JsonClass(generateAdapter = true)
data class KonemStatus(
  val shortName: String,
  val errors: Int,
  val received: Int,
  val sent: Int,
  val description: String
)


class KoneMessageReceiver(private val receive: (InetSocketAddress, KoneMesssage) -> Unit) :
  ReceiverHandler<String>() {
  private val logger = LoggerFactory.getLogger(KoneMessageReceiver::class.java)
  private val adaptor = moshi.adapter(KoneMesssage::class.java)

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
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  }

}
