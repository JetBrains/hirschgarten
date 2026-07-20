package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Serializer
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.junit.jupiter.api.Test
import kotlin.io.path.readText

class SnapshotKryoBuiltinRegistrationTest {
  @Test
  fun `builtin registration ids are stable`() {
    val goldFile = BazelPathManager.getTestFixturePath("sync/workspace/persistance/mvstore/builtinRegistrations.gold")
    val expected = goldFile.readText()
    val actual = builtinSnapshotRegistrations()
      .joinToString(
        separator = "\n",
        postfix = "\n",
      ) { reg -> "${reg.id} ${reg.type.name} ${reg.serializer.javaClass.name} ${reg.serializerVersion ?: "none"}" }
    if (expected != actual) {
      throw FileComparisonFailedError(
        "Kryo builtin serialization type IDs or serializer type has changed, " +
        "If change was intentional update `SNAPSHOT_FORMAT_VERSION` version " +
        "and then update gold `builtinRegistrations` file.",
        expected,
        actual,
        goldFile.toString(),
      )
    }
  }

  @Test
  fun `bazel-owned builtin serializers carry a binary format version`() {
    builtinSnapshotRegistrations()
      .filter {
        val name = it.type.name
        (name.startsWith("org.jetbrains") || name.startsWith("com.jetbrains"))
        && !it.serializer.javaClass.name.startsWith("com.esotericsoftware.kryo")
      }
      .forEach { registration ->
        check(registration.serializerVersion != null) {
          "Each type owned by plugin must use `VersionedKryoSerializer," +
          " `${registration.type}` should use `VersionedKryoSerializer`, right now ${registration.serializer}"
        }
      }
  }

  @VisibleForTesting
  fun builtinSnapshotRegistrations(): List<KryoRegistrationSummary> {
    val snapshotKryo = SnapshotKryo(SnapshotTypeUniverse(emptyList(), SnapshotManifest(sortedSetOf())))
    return snapshotKryo.pool.use { kryo ->
      (0 until KRYO_REGISTER_BEGIN_ID)
        .mapNotNull { id ->
          kryo.getRegistration(id)?.let { registration ->
            val serializer = registration.serializer
            KryoRegistrationSummary(
              id = id,
              type = registration.type,
              serializer = serializer,
              serializerVersion = (serializer as? VersionedKryoSerializer<*>)?.binaryFormatVersion,
            )
          }
        }
    }
  }

  data class KryoRegistrationSummary(val id: Int, val type: Class<*>, val serializer: Serializer<*>, val serializerVersion: Int?)
}
