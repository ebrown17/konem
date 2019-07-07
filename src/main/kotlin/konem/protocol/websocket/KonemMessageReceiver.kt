package konem.protocol.websocket


import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.sun.org.apache.bcel.internal.classfile.Unknown
import konem.netty.stream.ReceiverHandler
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.*


enum class KonemTypes {
  @Json(name = "heartbeat")
  HEARTBEAT,
  @Json(name = "status")
  STATUS,
  @Json(name = "unknown")
  UNKNOWN
}

class KonemMesssageAdaptor {

  fun prepReader(
    jsonReader: JsonReader,
    pairInfo: Pair<Array<String>, JsonReader.Options>
  ): JsonReader {
    val valid = pairInfo.first
    val options = pairInfo.second

    while (jsonReader.hasNext()) {
      when (jsonReader.peek()) {
        JsonReader.Token.BEGIN_OBJECT -> jsonReader.beginObject()
        JsonReader.Token.END_OBJECT -> jsonReader.endObject()
        JsonReader.Token.STRING -> jsonReader.nextString()
        JsonReader.Token.NAME -> {
          val num = jsonReader.selectName(options)
          if (num < 0 || num > valid.size) {
            jsonReader.skipName()
          } else {
            return jsonReader
          }
        }
        else -> jsonReader.skipValue()
      }
    }
    return jsonReader
  }

  fun clearReader(jsonReader: JsonReader): JsonReader {
    while (jsonReader.peek() != JsonReader.Token.END_DOCUMENT) {
      when (jsonReader.peek()) {
        JsonReader.Token.BEGIN_OBJECT -> jsonReader.beginObject()
        JsonReader.Token.END_OBJECT -> jsonReader.endObject()
        JsonReader.Token.NAME -> jsonReader.nextName()
        JsonReader.Token.STRING -> jsonReader.nextString()
        else -> {
          jsonReader.skipName()
        }
      }
    }

    return jsonReader
  }

  fun fillMap(
    jsonReader: JsonReader,
    pair: Pair<Array<String>, JsonReader.Options>
  ): MutableMap<String, Any?> {
    val mappy = mutableMapOf<String, Any?>()
    for (item in pair.first) {
      prepReader(jsonReader, pair)
      if (jsonReader.peek() == JsonReader.Token.STRING) {
        mappy[item] = jsonReader.nextString()
      } else if (jsonReader.peek() == JsonReader.Token.NUMBER) {
        mappy[item] = jsonReader.nextInt()
      }
      //TODO do rest token types
    }
    return mappy
  }

  @FromJson
  fun fromJson(jsonReader: JsonReader): KonemMesssage {
    val typeMap = fillMap(jsonReader, typeOption)
    val type: String = typeMap["type"] as String
    try {
      if (type == heartbeat) {
        return KonemMesssage(
          KonemTypes.HEARTBEAT,
          KonemHeartbeat.fromMap(fillMap(jsonReader, heartOpt))
        )
      } else if (type == status) {
        return KonemMesssage(KonemTypes.STATUS, KonemStatus.fromMap(fillMap(jsonReader, statusOpt)))
      }
      return KonemMesssage(KonemTypes.UNKNOWN, KonemTypes.UNKNOWN.toString())
    } finally {
      clearReader(jsonReader)
    }
  }

  @ToJson
  fun toJson(message: KonemMesssage) = when (message.type) {
    KonemTypes.HEARTBEAT -> {
      message
    }
    KonemTypes.STATUS -> {
      message
    }
    else -> {
      KonemMesssage(KonemTypes.UNKNOWN, message.message)
    }
  }

  companion object {
    const val heartbeat: String = "heartbeat"
    const val status: String = "status"

    val typeOption = Pair(
      arrayOf("type"),
      JsonReader.Options.of("type")
    )

    val heartOpt = Pair(arrayOf("sent"), JsonReader.Options.of("sent"))

    val statusOpt = Pair(
      arrayOf("shortName", "errors", "received", "sent", "description"),
      JsonReader.Options.of("shortName", "errors", "received", "sent", "description")
    )
  }
}

data class KonemMesssage(val type: KonemTypes, val message: Any) {

  companion object {
    fun Heartbeat(): KonemMesssage {
      return KonemMesssage(KonemTypes.HEARTBEAT, KonemHeartbeat(Date().toString()))
    }

    fun Status(
      shortName: String,
      errors: Int,
      received: Int,
      sent: Int,
      description: String
    ): KonemMesssage {
      return KonemMesssage(
        KonemTypes.STATUS,
        KonemStatus(shortName, errors, received, sent, description)
      )
    }

    fun Unknown(description: String): KonemMesssage {
      return KonemMesssage(KonemTypes.UNKNOWN, KonemUnknown(description))
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemHeartbeat(val sent: String = "Heartbeat Not Found") {
  companion object {
    fun fromMap(map: Map<String, Any?>): KonemHeartbeat {
      return KonemHeartbeat(map["sent"] as String)
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemUnknown(val description: String = "Unknown Message") {
  companion object {
    fun fromMap(map: Map<String, Any?>): KonemUnknown {
      return KonemUnknown(map["description"] as String)
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemStatus(
  val shortName: String = "",
  val errors: Int = -1,
  val received: Int = -1,
  val sent: Int = -1,
  val description: String = ""
) {
  companion object {
    fun fromMap(map: Map<String, Any?>): KonemStatus {
      return KonemStatus(
        map["shortName"] as String,
        map["errors"] as Int,
        map["received"] as Int,
        map["sent"] as Int,
        map["description"] as String
      )
    }
  }
}


class KoneMessageReceiver(private val receive: (InetSocketAddress, KonemMesssage) -> Unit) :
  ReceiverHandler<String>() {
  private val logger = LoggerFactory.getLogger(KoneMessageReceiver::class.java)
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
    val moshi: Moshi = Moshi.Builder().add(KonemMesssageAdaptor()).add(KotlinJsonAdapterFactory()).build()
  }

}
