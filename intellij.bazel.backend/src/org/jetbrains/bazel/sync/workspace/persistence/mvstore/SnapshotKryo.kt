package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Registration
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.util.MapReferenceResolver
import com.esotericsoftware.kryo.kryo5.util.Pool
import com.intellij.openapi.project.Project
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import java.nio.file.Path

internal const val KRYO_POOL_INSTANCES: Int = 8

// covers container layout, manifest codec, Kryo configuration, builtin registrations and the
// binary formats of all handwritten serializers - bump on any change to those
internal const val SNAPSHOT_FORMAT_VERSION: Int = 2

@ApiStatus.Internal
class SnapshotKryo(val universe: SnapshotTypeUniverse) {
  val manifest: SnapshotManifest
    get() = universe.manifest

  val pool: Pool<Kryo> = createPool(references = true)
  val valuePool: Pool<Kryo> = createPool(references = false)

  private fun createPool(references: Boolean): Pool<Kryo> =
    object : Pool<Kryo>(
      /* threadSafe = */ true,
      /* softReferences = */ false,
      /* maximumCapacity = */ KRYO_POOL_INSTANCES,
    ) {
      override fun create(): Kryo = createKryo(references)
    }

  private fun createKryo(references: Boolean): Kryo {
    val kryo = Kryo(
      /* classResolver = */ SnapshotClassResolver(),
      /* referenceResolver = */ if (references) MapReferenceResolver() else null,
    )
    kryo.references = references
    kryo.isRegistrationRequired = true
    kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

    // register builtins
    SnapshotSerializers.registerBuiltinTypes(kryo)

    // register open type set
    for ((registrationId, type, serializer) in universe.registrations) {
      if (serializer != null) {
        kryo.register(type, serializer, registrationId)
      }
      else {
        kryo.register(type, registrationId)
      }
    }
    return kryo
  }
}

internal class SnapshotClassResolver : DefaultClassResolver() {
  companion object {
    /**
     * Specify what types should be identified by their interface type ID,
     * it's necessary to avoid explicitly registering specific implementations of type like `Path`.
     *
     * **NOTE:** Each type here must register kryo default serializer.
     */
    private val OPEN_TYPES = arrayOf(Path::class.java, Label::class.java)
  }

  override fun getRegistration(type: Class<*>): Registration? {
    for (openType in OPEN_TYPES) {
      if (openType.isAssignableFrom(type)) {
        return super.getRegistration(openType)
      }
    }
    return super.getRegistration(type)
  }
}

@ApiStatus.Internal
data class SnapshotTypeRegistration(
  val registrationId: Int,
  val type: Class<*>,
  val serializer: VersionedKryoSerializer<*>?,
)

@ApiStatus.Internal
class SnapshotTypeUniverse(
  val registrations: List<SnapshotTypeRegistration>,
  val manifest: SnapshotManifest,
) {
  val type2Id: Reference2IntMap<Class<*>> = Reference2IntOpenHashMap(registrations.associate { it.type to it.registrationId })

  companion object {
    fun buildFromProject(project: Project): SnapshotTypeUniverse =
      build(WorkspaceTypeContributor.EP_NAME.extensionList.flatMap { it.contribute(project) })

    fun build(contributed: List<WorkspaceTypeEntry>): SnapshotTypeUniverse {
      val manifest = SnapshotTypeSchemas.buildManifest(contributed)
      val typeByFqn = contributed.associateBy({ it.fqn }, { it.type })
      val registrations = manifest.entries.map { (registrationId, fqn, schema) ->
        val type = typeByFqn.getValue(fqn)
        SnapshotTypeRegistration(
          registrationId = registrationId,
          type = type,
          serializer = when (schema) {
            SnapshotTypeSchema.Singleton -> SnapshotSerializers.singletonSerializerFor(type)
            is SnapshotTypeSchema.Enum, is SnapshotTypeSchema.Fields -> null
          },
        )
      }
      return SnapshotTypeUniverse(registrations, manifest)
    }
  }
}
