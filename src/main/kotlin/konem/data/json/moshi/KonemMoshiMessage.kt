package konem.data.json.moshi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

enum class KonemMoshiTypes {
  @Json(name = "heartbeat")
  HEARTBEAT,
  @Json(name = "status")
  STATUS,
  @Json(name = "unknown")
  UNKNOWN
}

@JsonClass(generateAdapter = true)
data class KonemMoshiHeartbeat(val sent: String = "Heartbeat Not Found") {
  companion object {
    const val sent = "sent"
    fun fromMap(map: Map<String, Any?>): KonemMoshiHeartbeat {
      return KonemMoshiHeartbeat(map[sent] as String)
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemMoshiUnknown(val description: String = "Unknown Message") {
  companion object {
    private const val description = "description"
    fun fromMap(map: Map<String, Any?>): KonemMoshiUnknown {
      return KonemMoshiUnknown(map[description] as String)
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemMoshiStatus(
  val shortName: String = "",
  val errors: Int = -1,
  val received: Int = -1,
  val sent: Int = -1,
  val description: String = ""
) {
  companion object {
    private const val shortName = "shortName"
    private const val errors = "errors"
    private const val received = "received"
    private const val sent = "sent"
    private const val description = "description"

    fun fromMap(map: Map<String, Any?>): KonemMoshiStatus {
      return KonemMoshiStatus(
        map[shortName] as String,
        map[errors] as Int,
        map[received] as Int,
        map[sent] as Int,
        map[description] as String
      )
    }
  }
}

data class KonemMoshiMesssage(val type: KonemMoshiTypes, val message: Any) {

  companion object {

    fun Heartbeat(): KonemMoshiMesssage {
      return KonemMoshiMesssage(
        KonemMoshiTypes.HEARTBEAT,
        KonemMoshiHeartbeat(Date().toString())
      )
    }

    fun Status(
      shortName: String,
      errors: Int,
      received: Int,
      sent: Int,
      description: String
    ): KonemMoshiMesssage {
      return KonemMoshiMesssage(
        KonemMoshiTypes.STATUS,
        KonemMoshiStatus(shortName, errors, received, sent, description)
      )
    }

    fun Unknown(description: String): KonemMoshiMesssage {
      return KonemMoshiMesssage(
        KonemMoshiTypes.UNKNOWN,
        KonemMoshiUnknown(description)
      )
    }
  }
}
