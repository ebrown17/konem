// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: konemwiremessage.proto
package konem.data.protobuf

import com.squareup.wire.EnumAdapter
import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import okio.ByteString

class KonemMessage(
  @field:WireField(
    tag = 1,
    adapter = "konem.data.protobuf.KonemMessage${'$'}MessageType#ADAPTER"
  )
  val messageType: MessageType? = null,
  @field:WireField(
    tag = 2,
    adapter = "konem.data.protobuf.KonemMessage${'$'}Unknown#ADAPTER"
  )
  val unknown: Unknown? = null,
  @field:WireField(
    tag = 3,
    adapter = "konem.data.protobuf.KonemMessage${'$'}Status#ADAPTER"
  )
  val status: Status? = null,
  @field:WireField(
    tag = 4,
    adapter = "konem.data.protobuf.KonemMessage${'$'}HeartBeat#ADAPTER"
  )
  val heartBeat: HeartBeat? = null,
  @field:WireField(
    tag = 5,
    adapter = "konem.data.protobuf.KonemMessage${'$'}Data#ADAPTER"
  )
  val data: Data? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<KonemMessage, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing {
    throw AssertionError()
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is KonemMessage) return false
    return unknownFields == other.unknownFields
        && messageType == other.messageType
        && unknown == other.unknown
        && status == other.status
        && heartBeat == other.heartBeat
        && data == other.data
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = messageType.hashCode()
      result = result * 37 + unknown.hashCode()
      result = result * 37 + status.hashCode()
      result = result * 37 + heartBeat.hashCode()
      result = result * 37 + data.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (messageType != null) result += """messageType=$messageType"""
    if (unknown != null) result += """unknown=$unknown"""
    if (status != null) result += """status=$status"""
    if (heartBeat != null) result += """heartBeat=$heartBeat"""
    if (data != null) result += """data=$data"""
    return result.joinToString(prefix = "KonemMessage{", separator = ", ", postfix = "}")
  }

  fun copy(
    messageType: MessageType? = this.messageType,
    unknown: Unknown? = this.unknown,
    status: Status? = this.status,
    heartBeat: HeartBeat? = this.heartBeat,
    data: Data? = this.data,
    unknownFields: ByteString = this.unknownFields
  ): KonemMessage = KonemMessage(messageType, unknown, status, heartBeat, data, unknownFields)

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<KonemMessage> = object : ProtoAdapter<KonemMessage>(
      FieldEncoding.LENGTH_DELIMITED, 
      KonemMessage::class
    ) {
      override fun encodedSize(value: KonemMessage): Int = 
        MessageType.ADAPTER.encodedSizeWithTag(1, value.messageType) +
        Unknown.ADAPTER.encodedSizeWithTag(2, value.unknown) +
        Status.ADAPTER.encodedSizeWithTag(3, value.status) +
        HeartBeat.ADAPTER.encodedSizeWithTag(4, value.heartBeat) +
        Data.ADAPTER.encodedSizeWithTag(5, value.data) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: KonemMessage) {
        MessageType.ADAPTER.encodeWithTag(writer, 1, value.messageType)
        Unknown.ADAPTER.encodeWithTag(writer, 2, value.unknown)
        Status.ADAPTER.encodeWithTag(writer, 3, value.status)
        HeartBeat.ADAPTER.encodeWithTag(writer, 4, value.heartBeat)
        Data.ADAPTER.encodeWithTag(writer, 5, value.data)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): KonemMessage {
        var messageType: MessageType? = null
        var unknown: Unknown? = null
        var status: Status? = null
        var heartBeat: HeartBeat? = null
        var data: Data? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> messageType = MessageType.ADAPTER.decode(reader)
            2 -> unknown = Unknown.ADAPTER.decode(reader)
            3 -> status = Status.ADAPTER.decode(reader)
            4 -> heartBeat = HeartBeat.ADAPTER.decode(reader)
            5 -> data = Data.ADAPTER.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return KonemMessage(
          messageType = messageType,
          unknown = unknown,
          status = status,
          heartBeat = heartBeat,
          data = data,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: KonemMessage): KonemMessage = value.copy(
        unknown = value.unknown?.let(Unknown.ADAPTER::redact),
        status = value.status?.let(Status.ADAPTER::redact),
        heartBeat = value.heartBeat?.let(HeartBeat.ADAPTER::redact),
        data = value.data?.let(Data.ADAPTER::redact),
        unknownFields = ByteString.EMPTY
      )
    }
  }

  enum class MessageType(
    override val value: Int
  ) : WireEnum {
    UNKNOWN(0),

    STATUS(1),

    HEARTBEAT(2),

    DATA(3);

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<MessageType> = object : EnumAdapter<MessageType>(
        MessageType::class
      ) {
        override fun fromValue(value: Int): MessageType = MessageType.fromValue(value)
      }

      @JvmStatic
      fun fromValue(value: Int): MessageType = when (value) {
        0 -> UNKNOWN
        1 -> STATUS
        2 -> HEARTBEAT
        3 -> DATA
        else -> throw IllegalArgumentException("""Unexpected value: $value""")
      }
    }
  }

  class Unknown(
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
    )
    val unknown: String? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<Unknown, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is Unknown) return false
      return unknownFields == other.unknownFields
          && unknown == other.unknown
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = unknown.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (unknown != null) result += """unknown=$unknown"""
      return result.joinToString(prefix = "Unknown{", separator = ", ", postfix = "}")
    }

    fun copy(unknown: String? = this.unknown, unknownFields: ByteString = this.unknownFields):
        Unknown = Unknown(unknown, unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<Unknown> = object : ProtoAdapter<Unknown>(
        FieldEncoding.LENGTH_DELIMITED, 
        Unknown::class
      ) {
        override fun encodedSize(value: Unknown): Int = 
          ProtoAdapter.STRING.encodedSizeWithTag(1, value.unknown) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: Unknown) {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.unknown)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): Unknown {
          var unknown: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> unknown = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Unknown(
            unknown = unknown,
            unknownFields = unknownFields
          )
        }

        override fun redact(value: Unknown): Unknown = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }

  class Status(
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
    )
    val shortName: String? = null,
    @field:WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    val errors: Int? = null,
    @field:WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    val received: Int? = null,
    @field:WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
    )
    val sent: Int? = null,
    @field:WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
    )
    val description: String? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<Status, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is Status) return false
      return unknownFields == other.unknownFields
          && shortName == other.shortName
          && errors == other.errors
          && received == other.received
          && sent == other.sent
          && description == other.description
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = shortName.hashCode()
        result = result * 37 + errors.hashCode()
        result = result * 37 + received.hashCode()
        result = result * 37 + sent.hashCode()
        result = result * 37 + description.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (shortName != null) result += """shortName=$shortName"""
      if (errors != null) result += """errors=$errors"""
      if (received != null) result += """received=$received"""
      if (sent != null) result += """sent=$sent"""
      if (description != null) result += """description=$description"""
      return result.joinToString(prefix = "Status{", separator = ", ", postfix = "}")
    }

    fun copy(
      shortName: String? = this.shortName,
      errors: Int? = this.errors,
      received: Int? = this.received,
      sent: Int? = this.sent,
      description: String? = this.description,
      unknownFields: ByteString = this.unknownFields
    ): Status = Status(shortName, errors, received, sent, description, unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<Status> = object : ProtoAdapter<Status>(
        FieldEncoding.LENGTH_DELIMITED, 
        Status::class
      ) {
        override fun encodedSize(value: Status): Int = 
          ProtoAdapter.STRING.encodedSizeWithTag(1, value.shortName) +
          ProtoAdapter.INT32.encodedSizeWithTag(2, value.errors) +
          ProtoAdapter.INT32.encodedSizeWithTag(3, value.received) +
          ProtoAdapter.INT32.encodedSizeWithTag(4, value.sent) +
          ProtoAdapter.STRING.encodedSizeWithTag(5, value.description) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: Status) {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.shortName)
          ProtoAdapter.INT32.encodeWithTag(writer, 2, value.errors)
          ProtoAdapter.INT32.encodeWithTag(writer, 3, value.received)
          ProtoAdapter.INT32.encodeWithTag(writer, 4, value.sent)
          ProtoAdapter.STRING.encodeWithTag(writer, 5, value.description)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): Status {
          var shortName: String? = null
          var errors: Int? = null
          var received: Int? = null
          var sent: Int? = null
          var description: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> shortName = ProtoAdapter.STRING.decode(reader)
              2 -> errors = ProtoAdapter.INT32.decode(reader)
              3 -> received = ProtoAdapter.INT32.decode(reader)
              4 -> sent = ProtoAdapter.INT32.decode(reader)
              5 -> description = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Status(
            shortName = shortName,
            errors = errors,
            received = received,
            sent = sent,
            description = description,
            unknownFields = unknownFields
          )
        }

        override fun redact(value: Status): Status = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }

  class HeartBeat(
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
    )
    val time: String? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<HeartBeat, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is HeartBeat) return false
      return unknownFields == other.unknownFields
          && time == other.time
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = time.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (time != null) result += """time=$time"""
      return result.joinToString(prefix = "HeartBeat{", separator = ", ", postfix = "}")
    }

    fun copy(time: String? = this.time, unknownFields: ByteString = this.unknownFields): HeartBeat =
        HeartBeat(time, unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<HeartBeat> = object : ProtoAdapter<HeartBeat>(
        FieldEncoding.LENGTH_DELIMITED, 
        HeartBeat::class
      ) {
        override fun encodedSize(value: HeartBeat): Int = 
          ProtoAdapter.STRING.encodedSizeWithTag(1, value.time) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: HeartBeat) {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.time)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): HeartBeat {
          var time: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> time = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return HeartBeat(
            time = time,
            unknownFields = unknownFields
          )
        }

        override fun redact(value: HeartBeat): HeartBeat = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }

  class Data(
    @field:WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
    )
    val data: String? = null,
    unknownFields: ByteString = ByteString.EMPTY
  ) : Message<Data, Nothing>(ADAPTER, unknownFields) {
    @Deprecated(
      message = "Shouldn't be used in Kotlin",
      level = DeprecationLevel.HIDDEN
    )
    override fun newBuilder(): Nothing {
      throw AssertionError()
    }

    override fun equals(other: Any?): Boolean {
      if (other === this) return true
      if (other !is Data) return false
      return unknownFields == other.unknownFields
          && data == other.data
    }

    override fun hashCode(): Int {
      var result = super.hashCode
      if (result == 0) {
        result = data.hashCode()
        super.hashCode = result
      }
      return result
    }

    override fun toString(): String {
      val result = mutableListOf<String>()
      if (data != null) result += """data=$data"""
      return result.joinToString(prefix = "Data{", separator = ", ", postfix = "}")
    }

    fun copy(data: String? = this.data, unknownFields: ByteString = this.unknownFields): Data =
        Data(data, unknownFields)

    companion object {
      @JvmField
      val ADAPTER: ProtoAdapter<Data> = object : ProtoAdapter<Data>(
        FieldEncoding.LENGTH_DELIMITED, 
        Data::class
      ) {
        override fun encodedSize(value: Data): Int = 
          ProtoAdapter.STRING.encodedSizeWithTag(1, value.data) +
          value.unknownFields.size

        override fun encode(writer: ProtoWriter, value: Data) {
          ProtoAdapter.STRING.encodeWithTag(writer, 1, value.data)
          writer.writeBytes(value.unknownFields)
        }

        override fun decode(reader: ProtoReader): Data {
          var data: String? = null
          val unknownFields = reader.forEachTag { tag ->
            when (tag) {
              1 -> data = ProtoAdapter.STRING.decode(reader)
              else -> reader.readUnknownField(tag)
            }
          }
          return Data(
            data = data,
            unknownFields = unknownFields
          )
        }

        override fun redact(value: Data): Data = value.copy(
          unknownFields = ByteString.EMPTY
        )
      }
    }
  }
}
