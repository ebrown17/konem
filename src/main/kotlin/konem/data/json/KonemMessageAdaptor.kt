package konem.data.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Options
import com.squareup.moshi.JsonReader.Token.*
import com.squareup.moshi.ToJson

class KonemMessageAdaptor {

  private fun prepReader(reader: JsonReader, pair: Pair<Array<String>, Options>): JsonReader {
    val valid = pair.first
    val options = pair.second

    while (reader.peek() != END_DOCUMENT) {
      when (reader.peek()) {
        BEGIN_OBJECT -> reader.beginObject()
        END_OBJECT -> reader.endObject()
        STRING -> reader.nextString()
        NAME -> {
          val num = reader.selectName(options)
          if (num < 0 || num > valid.size) {
            reader.skipName()
          } else {
            return reader
          }
        }
        else -> reader.skipValue()
      }
    }
    return reader
  }

  private fun clearReader(jsonReader: JsonReader): JsonReader {
    while (jsonReader.peek() != END_DOCUMENT) {
      when (jsonReader.peek()) {
        BEGIN_OBJECT -> jsonReader.beginObject()
        END_OBJECT -> jsonReader.endObject()
        NAME -> jsonReader.nextName()
        STRING -> jsonReader.nextString()
        else -> {
          jsonReader.skipName()
        }
      }
    }

    return jsonReader
  }

  private fun fillMap(
    reader: JsonReader,
    pair: Pair<Array<String>, Options>
  ): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    var count = 0
    for (item in pair.first) {
      prepReader(reader, pair)
      if (reader.peek() == STRING) {
        map[item] = reader.nextString()
      } else if (reader.peek() == NUMBER) {
        map[item] = reader.nextInt()
      }
      // TODO do rest token types
    }
    return map
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
      Options.of("type")
    )

    val heartOpt = Pair(arrayOf("sent"), Options.of("sent"))

    val statusOpt = Pair(
      arrayOf("shortName", "errors", "received", "sent", "description"),
      Options.of("shortName", "errors", "received", "sent", "description")
    )
  }
}