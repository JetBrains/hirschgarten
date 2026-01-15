package org.jetbrains.bazel.test.framework.target

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import kotlin.io.path.Path

object TestBuildTargetFactory {
  fun createSimpleJavaLibraryTarget(id: Label): RawBuildTarget =
    createSimpleTarget(
      id = id,
      kind = "java_library",
      ruleType = RuleType.LIBRARY,
      languages = setOf(LanguageClass.JAVA),
      data = JvmBuildTarget(javaVersion = "21"),
    )

  fun createSimpleKotlinLibraryTarget(id: Label): RawBuildTarget =
    createSimpleTarget(
      id = id,
      kind = "kt_jvm_library",
      ruleType = RuleType.LIBRARY,
      languages = setOf(LanguageClass.KOTLIN),
      data = KotlinBuildTarget(
        languageVersion = "1.8",
        apiVersion = "1.8",
        kotlincOptions = listOf(),
        jvmBuildTarget =
          JvmBuildTarget(
            javaVersion = "21",
          ),
        associates = listOf(),
      )
    )

  fun createSimpleTarget(
    id: Label,
    kind: String,
    ruleType: RuleType,
    languages: Set<LanguageClass>,
    data: BuildTargetData,
  ): RawBuildTarget = RawBuildTarget(
    id = id,
    tags = listOf(),
    dependencies = listOf(),
    kind = TargetKind(
      kindString = kind,
      ruleType = ruleType,
      languageClasses = languages,
    ),
    data = data,
    sources = listOf(),
    resources = listOf(),
    baseDirectory = Path("base/dir"),
  )
}
