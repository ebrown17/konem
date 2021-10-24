package konem.data.json

import java.util.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


@Serializable
abstract class Message

@Serializable
data class Data constructor(val data: String) : Message()

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


@Serializable
data class KonemMessage(@Polymorphic val message: Message)

class KonemMessageSerializer {

    val module = SerializersModule {
        polymorphic(Message::class) {
            subclass(Data::class,Data.serializer())
            subclass(Heartbeat::class,Heartbeat.serializer())
            subclass(Status::class,Status.serializer())
            subclass(Unknown::class,Unknown.serializer())
            default { Unknown.serializer() }
        }
    }

  private val serializer: KSerializer<KonemMessage>
    get() = KonemMessage.serializer()



  private val format: StringFormat
    get() = Json { serializersModule = module}

  fun toJson(msg: KonemMessage): String {
      return format.encodeToString((msg))
  }

  fun toKonemMessage(json: String): KonemMessage {
    return format.decodeFromString(json)
  }
}
