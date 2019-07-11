package konem.data.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.util.*
import konem.data.json.Message.*

@Serializable
sealed class Message {

  @Serializable
  data class Heartbeat constructor(val sent: String = Date().toString()) : Message()

  @Serializable
  data class Status constructor(
    val shortName: String = "",
    val errors: Int = -1,
    val received: Int = -1,
    val sent: Int = -1,
    val description: String = ""
  ) : Message()

  @Serializable
  data class Unknown constructor(val unknown: String = "Unknown Message") : Message()

}

@Serializable
data class KonemMessage(@Polymorphic val konemMessage: Message)

class KonemMessageSerializer {

  private val sealedModule = SerializersModule {
    polymorphic(Message::class) {
      Heartbeat::class with Heartbeat.serializer()
      Status::class with Status.serializer()
      Unknown::class with Unknown.serializer()
    }
  }

  private val serializer: KSerializer<KonemMessage>
    get() = KonemMessage.serializer()

  private val format: StringFormat
    get() = Json(context = sealedModule)

  fun toJson(msg: KonemMessage): String {
    return format.stringify(serializer, msg)
  }

  fun toKonemMessage(json: String): KonemMessage {
    return format.parse(serializer, json)
  }

}
