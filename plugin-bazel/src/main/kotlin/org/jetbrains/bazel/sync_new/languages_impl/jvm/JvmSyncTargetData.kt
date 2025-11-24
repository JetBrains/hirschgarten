package org.jetbrains.bazel.sync_new.languages_impl.jvm

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncClassTag
import org.jetbrains.bazel.sync_new.lang.SyncTargetData

@SyncClassTag(serialId = JvmSyncLanguage.LANGUAGE_TAG)
@Tagged
data class JvmSyncTargetData(
  @field:Tag(1)
  val jvmTarget: JvmTarget,

  @field:Tag(2)
  val priority: Int,

  @field:Tag(3)
  val compilerOptions: JvmCompilerOptions,

  @field:Tag(4)
  val binaryOutputs: List<BazelPath>,

  @field:Tag(5)
  val binaryMainClass: String
) : SyncTargetData

@Tagged
data class JvmCompilerOptions(
  @field:Tag(1)
  val javaVersion: String?,

  @field:Tag(2)
  val javacOpts: List<String>,

  @field:Tag(3)
  val javaHome: String?
)

@Tagged
data class JvmSourceFile(
  @field:Tag(1)
  val path: BazelPath,

  @field:Tag(2)
  val jvmPackagePrefix: String?,
)

@SealedTagged
sealed interface JvmTarget {

  @SealedTag(1)
  @Tagged
  data class SourceTarget(
    @field:Tag(1)
    val sources: List<JvmSourceFile>,
  ) : JvmTarget

  @SealedTag(2)
  @Tagged
  data class CompiledTarget(
    val classJars: List<BazelPath>,
    val sourceJars: List<BazelPath>,
    val interfaceJars: List<BazelPath>,
  ) : JvmTarget
}
