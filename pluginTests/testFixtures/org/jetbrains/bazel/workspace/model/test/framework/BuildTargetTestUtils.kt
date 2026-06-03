package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path

fun createRawBuildTarget(
  id: Label = Label.parse("//target"),
  dependencies: List<DependencyLabel> = emptyList(),
  kind: TargetKind = TargetKind(
    kind = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(LanguageClass.JAVA),
  ),
  sources: List<Path> = emptyList(),
  generatedSources: List<Path> = emptyList(),
  resources: List<Path> = emptyList(),
  baseDirectory: Path = Path("/base/dir"),
  data: List<BuildTargetData> = emptyList(),
): RawBuildTarget =
  RawBuildTarget(
    id = id,
    dependencies = dependencies,
    kind = kind,
    sources = sources,
    generatedSources = generatedSources,
    resources = resources,
    baseDirectory = baseDirectory,
    data = data,
  )
