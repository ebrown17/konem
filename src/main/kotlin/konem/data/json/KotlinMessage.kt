package konem.data.json

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import sun.plugin2.message.HeartbeatMessage
import java.util.*
import kotlin.system.measureTimeMillis

@Serializable
sealed class KotlinMessage {

  companion object{
    fun Heartbeat() : KotlinHeartbeat{
      return KotlinHeartbeat(Date().toString())
    }
  }

  @Serializable
  data class KotlinHeartbeat internal constructor(val sent: String) : KotlinMessage()

  @Serializable
  data class KotlinStatus internal constructor(
    val shortName: String = "",
    val errors: Int = -1,
    val received: Int = -1,
    val sent: Int = -1,
    val description: String = ""
  ) : KotlinMessage()

  @Serializable
  data class KotlinUnknown internal constructor(val unknown: String = "Unknown Message") : KotlinMessage()

}

@Serializable
data class MessageHolder(@Polymorphic val kotlinMessage: KotlinMessage)

class KotlinMessageSerializer {

  private val sealedModule = SerializersModule {
    polymorphic(KotlinMessage::class) {
      KotlinMessage.KotlinHeartbeat::class with KotlinMessage.KotlinHeartbeat.serializer()
      KotlinMessage.KotlinStatus::class with KotlinMessage.KotlinStatus.serializer()
      KotlinMessage.KotlinUnknown::class with KotlinMessage.KotlinUnknown.serializer()
    }
  }

  private val serializer: KSerializer<MessageHolder>
    get() = MessageHolder.serializer()

  private val format: StringFormat
    get() = Json(context = sealedModule)

  fun toJson(msg: MessageHolder): String {
    return format.stringify(serializer, msg)
  }

  fun toMessageHolder(json: String): MessageHolder {
    return format.parse(serializer, json)
  }

}


fun main() {

  val serializer = KotlinMessageSerializer()
  var count = 0

  val timeT = measureTimeMillis {
    for (i in 1..1000) {
      for (j in 1..100) {
        val kotlinBeat = MessageHolder(KotlinMessage.Heartbeat())
        val jsonBeat = serializer.toJson(kotlinBeat)
        serializer.toMessageHolder(jsonBeat)
        count++
      }
    }
  }

  println("Kotlin $count Took $timeT ms")

  val kotlinBeat = MessageHolder(KotlinMessage.Heartbeat())
  val jsonBeat = serializer.toJson(kotlinBeat)
  val back = serializer.toMessageHolder(jsonBeat)

  println(kotlinBeat)
  println(jsonBeat)
  println(back)

  val kotlinStatus = MessageHolder(KotlinMessage.KotlinStatus("Good Times",0,500,199,"All good here"))
  val jsonStatus = serializer.toJson(kotlinStatus)
  val statBack = serializer.toMessageHolder(jsonStatus)

  println(kotlinStatus)
  println(jsonStatus)
  println(statBack)
}