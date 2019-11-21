// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: konemwiremessage.proto
package konem.data.protobuf

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.missingRequiredFields
import kotlin.Any
import kotlin.AssertionError
import kotlin.Boolean
import kotlin.Deprecated
import kotlin.DeprecationLevel
import kotlin.Int
import kotlin.Nothing
import kotlin.String
import kotlin.jvm.JvmField
import okio.ByteString

class Unknown(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#STRING",
    label = WireField.Label.REQUIRED
  )
  val unknown: String,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<Unknown, Nothing>(ADAPTER, unknownFields) {
  @Deprecated(
    message = "Shouldn't be used in Kotlin",
    level = DeprecationLevel.HIDDEN
  )
  override fun newBuilder(): Nothing = throw AssertionError()

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is Unknown) return false
    return unknownFields == other.unknownFields
        && unknown == other.unknown
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + unknown.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    result += """unknown=$unknown"""
    return result.joinToString(prefix = "Unknown{", separator = ", ", postfix = "}")
  }

  fun copy(unknown: String = this.unknown, unknownFields: ByteString = this.unknownFields): Unknown
      = Unknown(unknown, unknownFields)

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
          unknown = unknown ?: throw missingRequiredFields(unknown, "unknown"),
          unknownFields = unknownFields
        )
      }

      override fun redact(value: Unknown): Unknown = value.copy(
        unknownFields = ByteString.EMPTY
      )
    }
  }
}
