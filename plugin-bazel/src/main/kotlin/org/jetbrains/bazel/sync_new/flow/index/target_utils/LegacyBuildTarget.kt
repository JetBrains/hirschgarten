package org.jetbrains.bazel.sync_new.flow.index.target_utils

import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer.Tag
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ClassTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTag
import org.jetbrains.bazel.sync_new.codec.kryo.EnumTagged
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTag
import org.jetbrains.bazel.sync_new.codec.kryo.SealedTagged
import org.jetbrains.bazel.sync_new.codec.kryo.Tagged
import org.jetbrains.bsp.protocol.BuildTargetTag
import java.nio.file.Path

object LegacyBuildTargetTags {
  const val NO_IDE: String = BuildTargetTag.NO_IDE
  const val MANUAL: String = BuildTargetTag.MANUAL
  const val INTELLIJ_PLUGIN: String = "intellij-plugin"
}

@Tagged
@ClassTag(2066606529)
internal class LegacyBuildTarget(
  @field:Tag(1)
  val label: Label,

  @field:Tag(2)
  val tags: List<String>,

  @field:Tag(3)
  val targetKind: LegacyTargetKind,

  @field:Tag(4)
  val baseDirectory: Path,

  @field:Tag(5)
  val data: LegacyBuildTargetData?,

  @field:Tag(6)
  val noBuild: Boolean,
)

@SealedTagged
@ClassTag(314053260)
internal sealed interface LegacyBuildTargetData

@Tagged
@ClassTag(378985339)
@SealedTag(1)
internal data class LegacyJvmTargetData(
  @field:Tag(1)
  val javaHome: Path?,

  @field:Tag(2)
  val javaVersion: String,

  @field:Tag(3)
  val javacOptions: List<String>,

  @field:Tag(4)
  val binaryOutputs: List<Path>,
) : LegacyBuildTargetData

@Tagged
@ClassTag(132555074)
@SealedTag(2)
internal data class LegacyKotlinTargetData(
  @field:Tag(1)
  val languageVersion: String,

  @field:Tag(2)
  val apiVersion: String,

  @field:Tag(3)
  val kotlincOptions: List<String>,

  @field:Tag(4)
  val associates: List<Label>,

  @field:Tag(5)
  val jvmBuildTarget: LegacyJvmTargetData?,
) : LegacyBuildTargetData

@Tagged
@ClassTag(248980144)
internal class LegacyTargetKind(
  @field:Tag(1)
  val kind: String,

  @field:Tag(2)
  val languageClasses: Set<LegacyLanguageClass>,

  @field:Tag(3)
  val ruleType: LegacyRuleType,
)

@EnumTagged
@ClassTag(668920970)
internal enum class LegacyLanguageClass {
  @EnumTag(1)
  JAVA,

  @EnumTag(2)
  KOTLIN
}

@EnumTagged
@ClassTag(1230887122)
internal enum class LegacyRuleType {
  @EnumTag(1)
  TEST,

  @EnumTag(2)
  BINARY,

  @EnumTag(3)
  LIBRARY,

  @EnumTag(4)
  UNKNOWN,
}
