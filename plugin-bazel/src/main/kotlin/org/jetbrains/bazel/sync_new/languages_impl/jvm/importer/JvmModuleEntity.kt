package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.serializers.CollectionSerializer.BindCollection
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.Bind
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.KryoNIOPathSerializer
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntity
import org.jetbrains.bazel.sync_new.lang.store.IncrementalResourceId
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmToolchain
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putLabel
import java.nio.file.Path

@SealedTagged
@ClassTag(1052849018)
sealed interface JvmModuleEntity : IncrementalEntity {

  // close representation of org.jetbrains.bsp.protocol.RawBuildTarget with only necessary stuff
  @SealedTag(1)
  @Tagged
  @ClassTag(1652176529)
  data class LegacySourceModule(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
    val baseDirectory: Path,

    @field:Tag(3)
    val label: Label,

    @field:Tag(4)
    val dependencies: Set<Label>,

    @field:Tag(5)
    val sources: List<JvmSourceItem>,

    @field:Tag(6)
    @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class)
    val resources: MutableList<Path>,

    @field:Tag(7)
    val legacyKotlinData: LegacyKotlinTargetData?,

    @field:Tag(8)
    val legacyJvmData: LegacyJvmTargetData?,
  ) : JvmModuleEntity

  // close representation of org.jetbrains.bsp.protocol.LibraryItem
  // TODO: maven coordinates
  @SealedTag(2)
  @Tagged
  @ClassTag(158927477)
  data class LegacyLibraryModule(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    val label: Label,

    @field:Tag(3)
    val dependencies: Set<Label>,

    @field:Tag(4)
    @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class, )
    val interfaceJars: HashSet<Path>,

    @field:Tag(5)
    @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class, )
    val classJars: HashSet<Path>,

    @field:Tag(6)
    @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class, )
    val sourceJars: HashSet<Path>,

    @field:Tag(7)
    val isFromInternalTarget: Boolean,

    @field:Tag(8)
    val isLowPriority: Boolean,
  ) : JvmModuleEntity

  @SealedTag(3)
  @Tagged
  @ClassTag(2026133597)
  data class VertexDeps(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    val deps: Set<Label>,
  ) : JvmModuleEntity

  @SealedTag(4)
  @Tagged
  @ClassTag(1564322548)
  data class JdepsCache(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class)
    val myJdeps: HashSet<Path>,
  ) : JvmModuleEntity

  @SealedTag(5)
  @Tagged
  @ClassTag(155046271)
  data class DefaultToolchain(
    @field:Tag(1)
    override val resourceId: Int,

    @field:Tag(2)
    val toolchain: JvmToolchain?,
  ) : JvmModuleEntity

}

@Tagged
@ClassTag(1812664773)
data class JvmSourceItem(
  @field:Tag(1)
  @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
  val path: Path,

  @field:Tag(2)
  val generated: Boolean,

  @field:Tag(3)
  val jvmPackagePrefix: String?,
)

// org.jetbrains.bsp.protocol.KotlinBuildTarget
@Tagged
@ClassTag(1152799515)
data class LegacyKotlinTargetData(
  @field:Tag(1)
  val languageVersion: String,

  @field:Tag(2)
  val apiVersion: String,

  @field:Tag(3)
  val kotlincOptions: List<String>,

  @field:Tag(4)
  val associates: List<Label>,

  @field:Tag(5)
  val moduleName: String?,
)

// org.jetbrains.bsp.protocol.JvmBuildTarget
@Tagged
@ClassTag(1561055026)
data class LegacyJvmTargetData(
  @field:Tag(1)
  @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
  val javaHome: Path?,

  @field:Tag(2)
  val javaVersion: String,

  @field:Tag(3)
  val javacOpts: List<String>,

  @field:Tag(4)
  @field:BindCollection(elementClass = Path::class, elementSerializer = KryoNIOPathSerializer::class)
  val binaryOutputs: MutableList<Path>,

  @field:Tag(5)
  val toolchain: JvmToolchain?,
)

@SealedTagged
@ClassTag(1155347504)
sealed interface JvmResourceId : IncrementalResourceId {

  @SealedTag(1)
  @Tagged
  @ClassTag(1985969820)
  data class VertexReference(
    @field:Tag(1)
    val vertexId: Int,
  ) : JvmResourceId

  @SealedTag(2)
  @Tagged
  @ClassTag(1906742946)
  data class VertexDeps(
    @field:Tag(1)
    val label: Label,
  ) : JvmResourceId

  @SealedTag(3)
  @Tagged
  @ClassTag(541635690)
  data class JdepsLibrary(
    @field:Tag(1)
    val libraryName: String,
  ) : JvmResourceId

  @SealedTag(4)
  @Tagged
  @ClassTag(792553685)
  data class JdepsCache(
    @field:Tag(1)
    val vertexId: Int,
  ) : JvmResourceId

  @SealedTag(6)
  @Tagged
  @ClassTag(2134784395)
  data class CompiledLibrary(
    @field:Tag(1)
    val owner: Int,

    @field:Tag(2)
    val name: String,
  ) : JvmResourceId

  @SealedTag(7)
  @Tagged
  @ClassTag(1110053395)
  data object DefaultToolchain : JvmResourceId

  @SealedTag(8)
  @Tagged
  @ClassTag(542285109)
  data object KotlinStdlib : JvmResourceId

  @SealedTag(9)
  @Tagged
  @ClassTag(312458415)
  data class GeneratedLibrary(
    @field:Tag(1)
    val owner: Int,

    @field:Tag(2)
    val name: String,
  ) : JvmResourceId

}

fun JvmResourceId.hash(): HashValue128 = hash h@{
  when (this@hash) {

    is JvmResourceId.CompiledLibrary -> {
      putInt(2)
      putInt(owner)
      putString(name)
    }

    is JvmResourceId.JdepsLibrary -> {
      putInt(3)
      putString(libraryName)
    }

    is JvmResourceId.JdepsCache -> {
      putInt(4)
      putInt(vertexId)
    }

    is JvmResourceId.VertexDeps -> {
      putInt(5)
      putLabel(label)
    }

    is JvmResourceId.VertexReference -> {
      putInt(6)
      putInt(vertexId)
    }

    JvmResourceId.DefaultToolchain -> {
      putInt(7)
    }

    JvmResourceId.KotlinStdlib -> {
      putInt(8)
    }

    is JvmResourceId.GeneratedLibrary -> {
      putInt(9)
      putInt(owner)
      putString(name)
    }
  }
}
