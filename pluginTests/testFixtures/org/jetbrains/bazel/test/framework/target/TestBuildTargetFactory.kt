package org.jetbrains.bazel.test.framework.target

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.KotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget
import kotlin.io.path.Path

object TestBuildTargetFactory {
  fun createSimpleJavaLibraryTarget(id: Label): RawBuildTarget =
    createSimpleTarget(
      id = id,
      kind = "java_library",
      ruleType = RuleType.LIBRARY,
      languages = setOf(JavaLanguageClass.JAVA),
      data = listOf(JvmBuildTarget()),
    )

  fun createSimpleKotlinLibraryTarget(id: Label): RawBuildTarget =
    createSimpleTarget(
      id = id,
      kind = "kt_jvm_library",
      ruleType = RuleType.LIBRARY,
      languages = setOf(JavaLanguageClass.KOTLIN),
      data = listOf(
        KotlinBuildTarget(
          languageVersion = "1.8",
          apiVersion = "1.8",
          kotlincOptions = listOf(),
          associates = listOf(),
        ),
        JvmBuildTarget(),
      ),
    )

  fun createSimpleTarget(
    id: Label,
    kind: String,
    ruleType: RuleType,
    languages: Set<LanguageClass>,
    data: List<BuildTargetData>,
  ): RawBuildTarget = RawBuildTarget(
    key = WorkspaceTargetKey(label = id),
    dependencies = emptyList(),
    kind = TargetKind(
      kind = kind,
      ruleType = ruleType,
      languageClasses = languages,
    ),
    data = data,
    sources = SourceFileCollection.EMPTY,
    generatedSources = SourceFileCollection.EMPTY,
    resources = SourceFileCollection.EMPTY,
    baseDirectory = Path("base/dir"),
  )
}
