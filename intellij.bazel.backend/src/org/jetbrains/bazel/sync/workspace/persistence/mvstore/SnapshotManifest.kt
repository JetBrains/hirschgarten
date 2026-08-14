package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import org.jetbrains.annotations.ApiStatus
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.SortedSet

@ApiStatus.Internal
sealed interface SnapshotTypeSchema {
  data class Fields(val fields: List<Field>) : SnapshotTypeSchema {
    data class Field(val name: String, val typeFqn: String)
  }

  data class Enum(val constants: List<String>) : SnapshotTypeSchema

  data object Singleton : SnapshotTypeSchema
}

@ApiStatus.Internal
data class SnapshotManifestEntry(
  val registrationId: Int,
  val fqn: String,
  val schema: SnapshotTypeSchema,
) {
  companion object {
    internal val COMPARATOR = compareBy<SnapshotManifestEntry> { it.registrationId }
      .thenComparing { it.fqn }
  }
}

@ApiStatus.Internal
data class SnapshotManifest(val entries: SortedSet<SnapshotManifestEntry>) {

  // compute sha-256 fingerprint of serialization schema
  fun fingerprint(): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    DigestOutputStream(OutputStream.nullOutputStream(), digest)
      .let { stream ->
        Output(stream, DIGEST_BUFFER_SIZE)
          .use { output -> writeTo(output) }
      }
    return digest.digest()
  }

  fun writeTo(output: Output) {
    output.writeVarInt(entries.size, true)
    for ((registrationId, fqn, schema) in entries) {
      output.writeVarInt(registrationId, true)
      output.writeString(fqn)
      when (schema) {
        is SnapshotTypeSchema.Fields -> {
          output.writeByte(KIND_FIELDS)
          output.writeVarInt(schema.fields.size, true)
          for ((name, typeFqn) in schema.fields) {
            output.writeString(name)
            output.writeString(typeFqn)
          }
        }

        is SnapshotTypeSchema.Enum -> {
          output.writeByte(KIND_ENUM)
          output.writeVarInt(schema.constants.size, true)
          schema.constants.forEach(output::writeString)
        }

        SnapshotTypeSchema.Singleton -> {
          output.writeByte(KIND_SINGLETON)
        }
      }
    }
  }

  companion object {
    private const val KIND_FIELDS = 0
    private const val KIND_ENUM = 1
    private const val KIND_SINGLETON = 2
    private const val DIGEST_BUFFER_SIZE = 4096

    fun readFrom(input: Input): SnapshotManifest {
      val count = input.readVarInt(true)
      val entries = ArrayList<SnapshotManifestEntry>(count)
      repeat(count) {
        val registrationId = input.readVarInt(true)
        val fqn = checkNotNull(input.readString())
        val schema = when (val kind = input.readByte().toInt()) {
          KIND_FIELDS -> {
            val fieldCount = input.readVarInt(true)
            SnapshotTypeSchema.Fields(
              List(fieldCount) {
                SnapshotTypeSchema.Fields.Field(
                  name = checkNotNull(input.readString()),
                  typeFqn = checkNotNull(input.readString()),
                )
              },
            )
          }

          KIND_ENUM -> {
            val constantCount = input.readVarInt(true)
            SnapshotTypeSchema.Enum(List(constantCount) { checkNotNull(input.readString()) })
          }

          KIND_SINGLETON -> SnapshotTypeSchema.Singleton

          else -> error("Unknown manifest schema kind: $kind")
        }
        entries += SnapshotManifestEntry(registrationId, fqn, schema)
      }
      return SnapshotManifest(entries.toSortedSet(SnapshotManifestEntry.COMPARATOR))
    }
  }
}

internal fun SnapshotManifest.describeChangesAgainst(current: SnapshotManifest): List<String> {
  val reasons = mutableListOf<String>()
  val storedByFqn = entries.associateBy { it.fqn }
  val currentByFqn = current.entries.associateBy { it.fqn }

  (storedByFqn.keys - currentByFqn.keys).forEach { reasons += "type removed: $it" }
  (currentByFqn.keys - storedByFqn.keys).forEach { reasons += "type added: $it" }

  for ((storedId, fqn, storedSchema) in entries) {
    val currentEntry = currentByFqn[fqn] ?: continue
    if (storedId != currentEntry.registrationId) {
      reasons += "registration id of $fqn changed: $storedId -> ${currentEntry.registrationId}"
      continue
    }
    if (storedSchema != currentEntry.schema) {
      reasons += describeSchemaChange(fqn, storedSchema, currentEntry.schema)
    }
  }
  return reasons
}

private fun describeSchemaChange(fqn: String, stored: SnapshotTypeSchema, current: SnapshotTypeSchema): String {
  if (stored::class != current::class) {
    return "$fqn: kind changed ${stored.kindName()} -> ${current.kindName()}"
  }
  return when (stored) {
    is SnapshotTypeSchema.Fields -> {
      val storedByName = stored.fields.associateBy { it.name }
      val currentByName = (current as SnapshotTypeSchema.Fields).fields.associateBy { it.name }
      val details = buildList {
        (storedByName.keys - currentByName.keys).forEach { add("field removed: $it") }
        (currentByName.keys - storedByName.keys).forEach { add("field added: $it") }
        for ((name, storedField) in storedByName) {
          val currentField = currentByName[name] ?: continue
          if (storedField.typeFqn != currentField.typeFqn) {
            add("field $name type changed: ${storedField.typeFqn} -> ${currentField.typeFqn}")
          }
        }
      }
      "$fqn: ${details.joinToString(", ")}"
    }

    is SnapshotTypeSchema.Enum ->
      "$fqn: enum constants changed ${stored.constants} -> ${(current as SnapshotTypeSchema.Enum).constants}"

    SnapshotTypeSchema.Singleton -> "$fqn: schema changed"
  }
}

private fun SnapshotTypeSchema.kindName(): String = when (this) {
  is SnapshotTypeSchema.Fields -> "FIELDS"
  is SnapshotTypeSchema.Enum -> "ENUM"
  SnapshotTypeSchema.Singleton -> "SINGLETON"
}
