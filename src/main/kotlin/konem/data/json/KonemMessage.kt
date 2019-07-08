package konem.data.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

enum class KonemTypes {
  @Json(name = "heartbeat")
  HEARTBEAT,
  @Json(name = "status")
  STATUS,
  @Json(name = "unknown")
  UNKNOWN
}

@JsonClass(generateAdapter = true)
data class KonemHeartbeat(val sent: String = "Heartbeat Not Found") {
  companion object {
    const val sent = "sent"
    fun fromMap(map: Map<String, Any?>): KonemHeartbeat {
      return KonemHeartbeat(map[sent] as String)
    }
  }
}

@JsonClass(generateAdapter = true)
data class KonemUnknown(val description: String = "Unknown Message") {
  companion object {
    private const val description = "description"
    fun fromMap(map: Map<String, Any?>): KonemUnknown {
      return KonemUnknown(map[description] as String)
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
    private const val shortName = "shortName"
    private const val errors = "errors"
    private const val received = "received"
    private const val sent = "sent"
    private const val description = "description"

    fun fromMap(map: Map<String, Any?>): KonemStatus {
      return KonemStatus(
        map[shortName] as String,
        map[errors] as Int,
        map[received] as Int,
        map[sent] as Int,
        map[description] as String
      )
    }
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
        KonemTypes.STATUS, KonemStatus(shortName, errors, received, sent, description)
      )
    }

    fun Unknown(description: String): KonemMesssage {
      return KonemMesssage(KonemTypes.UNKNOWN, KonemUnknown(description))
    }
  }
}
