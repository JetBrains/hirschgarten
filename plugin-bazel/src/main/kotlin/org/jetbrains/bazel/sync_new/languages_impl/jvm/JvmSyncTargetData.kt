package org.jetbrains.bazel.sync_new.languages_impl.jvm

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import org.jetbrains.bazel.sync_new.lang.SyncClassTag
import org.jetbrains.bazel.sync_new.lang.SyncTargetData
import java.nio.file.Path

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
  val outputs: JvmOutputs,

  @field:Tag(5)
  val generatedOutputs: JvmOutputs,

  @field:Tag(6)
  val binaryMainClass: String?,
) : SyncTargetData

@Tagged
data class JvmCompilerOptions(
  @field:Tag(1)
  val javaVersion: String?,

  @field:Tag(2)
  val javacOpts: List<String>,

  @field:Tag(3)
  val javaHome: Path?,
)

@Tagged
data class JvmSourceFile(
  @field:Tag(1)
  val path: BazelPath,

  @field:Tag(2)
  var jvmPackagePrefix: String? = null,

  @field:Tag(3)
  val priority: Int,

  @field:Tag(4)
  val generated: Boolean
)

@Tagged
data class JvmTarget(
  @field:Tag(1)
  val sources: List<JvmSourceFile>,

  @field:Tag(2)
  val generatedSources: List<BazelPath>,

  @field:Tag(3)
  val jdeps: List<BazelPath>,

  @field:Tag(4)
  val hasApiGeneratingPlugin: Boolean
)

@Tagged
data class JvmOutputs(
  @field:Tag(1)
  val classJars: List<BazelPath>,
  @field:Tag(2)
  val srcJars: List<BazelPath>,
  @field:Tag(3)
  val iJars: List<BazelPath>,
)
