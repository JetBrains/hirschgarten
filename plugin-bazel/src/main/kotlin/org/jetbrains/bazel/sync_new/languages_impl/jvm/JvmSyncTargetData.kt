package org.jetbrains.bazel.sync_new.languages_impl.jvm

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.Bind
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.KryoNIOPathSerializer
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncClassTag
import org.jetbrains.bazel.sync_new.lang.SyncTargetData
import java.nio.file.Path

@SyncClassTag(serialId = JvmSyncLanguage.LANGUAGE_TAG)
@Tagged
@ClassTag(1463073917)
data class JvmSyncTargetData(
  @field:Tag(1)
  val jvmTarget: JvmTarget,

  @field:Tag(2)
  val priority: Int,

  @field:Tag(3)
  val compilerOptions: JvmCompilerOptions,

  @field:Tag(4)
  val outputs: JvmOutputs,

  @field:Tag(5)
  val generatedOutputs: JvmOutputs,

  @field:Tag(6)
  val binaryMainClass: String?,

  @field:Tag(7)
  val toolchain: JvmToolchain?,
) : SyncTargetData

@Tagged
@ClassTag(1988052902)
data class JvmCompilerOptions(
  @field:Tag(1)
  val javaVersion: String?,

  @field:Tag(2)
  val javacOpts: List<String>,

  @field:Tag(3)
  @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
  val javaHome: Path?,
)

@Tagged
@ClassTag(1717490412)
data class JvmSourceFile(
  @field:Tag(1)
  val path: BazelPath,

  @field:Tag(2)
  var jvmPackagePrefix: String? = null,

  @field:Tag(3)
  val priority: Int,

  @field:Tag(4)
  val generated: Boolean,
)

@Tagged
@ClassTag(1453589755)
data class JvmTarget(
  @field:Tag(1)
  val sources: List<JvmSourceFile>,

  @field:Tag(2)
  val generatedSources: List<BazelPath>,

  @field:Tag(3)
  val jdeps: List<BazelPath>,

  @field:Tag(4)
  val hasApiGeneratingPlugin: Boolean,
)

@Tagged
@ClassTag(1218804259)
data class JvmOutputs(
  @field:Tag(1)
  val classJars: List<BazelPath>,
  @field:Tag(2)
  val srcJars: List<BazelPath>,
  @field:Tag(3)
  val iJars: List<BazelPath>,
)

@Tagged
@ClassTag(1715118049)
data class JvmToolchain(
  @field:Tag(1)
  val kind: JvmToolchainKind,

  @field:Tag(2)
  @field:Bind(valueClass = Path::class, serializer = KryoNIOPathSerializer::class)
  val javaHome: Path,
)

@EnumTagged
@ClassTag(1012592358)
enum class JvmToolchainKind {
  @EnumTag(1)
  BOOT_CLASSPATH,

  @EnumTag(2)
  RUNTIME,

  @EnumTag(3)
  TOOLCHAIN
}
