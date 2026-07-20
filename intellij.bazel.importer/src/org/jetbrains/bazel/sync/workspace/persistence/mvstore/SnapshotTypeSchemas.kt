package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import java.lang.reflect.Modifier

// start from larger registration id to keep room for builtin types
@ApiStatus.Internal
const val KRYO_REGISTER_BEGIN_ID: Int = 256

internal class SnapshotUniverseValidationException(problems: List<String>) :
  IllegalStateException("Workspace snapshot type universe is not serializable:\n${problems.joinToString("\n")}")

internal object SnapshotTypeSchemas {

  fun buildManifest(contributed: List<WorkspaceTypeEntry>): SnapshotManifest {
    val problems = mutableListOf<String>()
    contributed.groupBy { it.fqn }
      .filterValues { it.size > 1 }
      .keys.forEach { problems += "duplicate contribution of `$it`" }

    val schemas = mutableMapOf<String, SnapshotTypeSchema>()
    for ((fqn, type) in contributed) {
      if (type.isAnonymousClass || type.isLocalClass || type.isSynthetic) {
        problems += "`$fqn`: anonymous, local and synthetic (lambda) classes cannot be persisted"
        continue
      }
      schemas[fqn] = when {
        type.isEnum -> SnapshotTypeSchema.Enum(type.enumConstants!!.map { (it as Enum<*>).name })
        isKotlinObject(type) -> SnapshotTypeSchema.Singleton

        Collection::class.java.isAssignableFrom(type) || Map::class.java.isAssignableFrom(type) -> {
          problems += "`$fqn`: collection types cannot be contributed"
          continue
        }

        else -> SnapshotTypeSchema.Fields(collectSerializedFields(type))
      }
    }
    if (problems.isNotEmpty()) {
      throw SnapshotUniverseValidationException(problems)
    }

    return SnapshotManifest(
      entries = contributed
        .mapIndexed { index, entry ->
          SnapshotManifestEntry(
            registrationId = KRYO_REGISTER_BEGIN_ID + index,
            fqn = entry.fqn,
            schema = schemas.getValue(entry.fqn),
          )
        }
        .toSortedSet(SnapshotManifestEntry.COMPARATOR),
    )
  }

  private fun collectSerializedFields(type: Class<*>): List<SnapshotTypeSchema.Fields.Field> {
    val collected = mutableListOf<SnapshotTypeSchema.Fields.Field>()
    var current: Class<*>? = type
    while (current != null && current != Any::class.java) {
      for (field in current.declaredFields) {
        val modifiers = field.modifiers
        if (Modifier.isStatic(modifiers) || field.isSynthetic || Modifier.isTransient(modifiers)) {
          continue
        }
        collected += SnapshotTypeSchema.Fields.Field(name = field.name, typeFqn = field.type.name)
      }
      current = current.superclass
    }
    return collected.sortedBy { it.name }
  }

  private fun isKotlinObject(type: Class<*>): Boolean {
    val instance = type.declaredFields.firstOrNull { it.name == "INSTANCE" } ?: return false
    return Modifier.isStatic(instance.modifiers) && Modifier.isFinal(instance.modifiers) && instance.type == type
  }
}
