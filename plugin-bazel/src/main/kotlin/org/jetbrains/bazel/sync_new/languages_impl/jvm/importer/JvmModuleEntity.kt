package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntity
import org.jetbrains.bazel.sync_new.lang.store.IncrementalResourceId
import org.jetbrains.bazel.sync_new.lang.store.NonHashable
import org.jetbrains.bazel.sync_new.lang.store.emptyInt
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putResolvedLabel
import java.nio.file.Path

@SealedTagged
sealed interface JvmModuleEntity : IncrementalEntity {

  // close representation of org.jetbrains.bsp.protocol.RawBuildTarget with only necessary stuff
  @SealedTag(1)
  @Tagged
  data class LegacySourceModule(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    val label: Label,

    @field:Tag(3)
    val dependencies: List<Label>,

    @field:Tag(4)
    val sources: List<JvmSourceItem>,

    @field:Tag(5)
    val resources: List<Path>,

    @field:Tag(6)
    val legacyKotlinData: LegacyKotlinTargetData?,

    @field:Tag(7)
    val legacyJvmData: LegacyJvmTargetData?
  ) : JvmModuleEntity

  // close representation of org.jetbrains.bsp.protocol.LibraryItem
  // TODO: maven coordinates
  @SealedTag(2)
  @Tagged
  data class LegacyLibraryModule(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    val label: Label,

    @field:Tag(3)
    val dependencies: Set<Label>,

    @field:Tag(4)
    val interfaceJars: Set<Path>,

    @field:Tag(5)
    val classJars: Set<Path>,

    @field:Tag(6)
    val sourceJars: Set<Path>,

    @field:Tag(7)
    val isFromInternalTarget: Boolean,

    @field:Tag(8)
    val isLowPriority: Boolean,
  ) : JvmModuleEntity

  @SealedTag(3)
  @Tagged
  data class VertexDeps(
    override val resourceId: Int,
    val deps: Set<Label>,
  ) : JvmModuleEntity

  @SealedTag(4)
  @Tagged
  data class JdepsTransitive(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(1)
    val allTransitiveDeps: Set<Path>
  ) : JvmModuleEntity

}

@Tagged
data class JvmSourceItem(
  @field:Tag(1)
  val path: Path,

  @field:Tag(2)
  val generated: Boolean,

  @field:Tag(3)
  val jvmPackagePrefix: String?,
)

// org.jetbrains.bsp.protocol.KotlinBuildTarget
@Tagged
data class LegacyKotlinTargetData(
  @field:Tag(1)
  val languageVersion: String,

  @field:Tag(2)
  val apiVersion: String,

  @field:Tag(3)
  val kotlincOptions: List<String>,

  @field:Tag(4)
  val associates: List<Label>,
)

// org.jetbrains.bsp.protocol.JvmBuildTarget
@Tagged
data class LegacyJvmTargetData(
  @field:Tag(1)
  val javaHome: Path?,

  @field:Tag(2)
  val javaVersion: String,

  @field:Tag(3)
  val javacOpts: List<String>,

  @field:Tag(4)
  val binaryOutputs: List<Path>,
)

@SealedTagged
sealed interface JvmResourceId : IncrementalResourceId {

  @SealedTag(1)
  @Tagged
  data class VertexReference(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val vertexId: Int,
  ) : JvmResourceId

  @SealedTag(10)
  @Tagged
  data class VertexDeps(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val label: Label,
  ) : JvmResourceId

  @SealedTag(2)
  @Tagged
  data class JdepsLibrary(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val libraryName: String,
  ) : JvmResourceId

  data class JdepsTransitive(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val vertexId: Int,
  ) : JvmResourceId

  @SealedTag(3)
  @Tagged
  data class AnnotationProcessorLibrary(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val owner: Int,
  ) : JvmResourceId

  @SealedTag(4)
  @Tagged
  data class CompiledLibrary(
    @field:Tag(1)
    override val id: NonHashable<Int> = emptyInt(),

    @field:Tag(2)
    val owner: Int,
  ) : JvmResourceId

}

fun JvmResourceId.hash(): HashValue128 = hash h@{
  when (this@hash) {
    is JvmResourceId.AnnotationProcessorLibrary -> {
      putInt(1)
      putInt(owner)
    }
    is JvmResourceId.CompiledLibrary -> {
      putInt(2)
      putInt(owner)
    }
    is JvmResourceId.JdepsLibrary -> {
      putInt(3)
      putString(libraryName)
    }
    is JvmResourceId.JdepsTransitive -> {
      putInt(4)
      putInt(vertexId)
    }
    is JvmResourceId.VertexDeps -> {
      putInt(5)
      putResolvedLabel(label.assumeResolved())
    }
    is JvmResourceId.VertexReference -> {
      putInt(6)
      putInt(vertexId)
    }
  }
}
