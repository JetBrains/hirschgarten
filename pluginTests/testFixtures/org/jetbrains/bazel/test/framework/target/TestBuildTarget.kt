package org.jetbrains.bazel.test.framework.target

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.BuildTargetInfo
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.copy

@Suppress("TestFunctionName")
fun TestBuildTarget(
  key: WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse("//target")),
  dependencies: List<DependencyLabel> = emptyList(),
  kind: TargetKind = TargetKind(
    kind = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(JavaLanguageClass.JAVA),
  ),
  sources: OutputLocationCollection = OutputLocationCollection.EMPTY,
  resources: OutputLocationCollection = OutputLocationCollection.EMPTY,
  data: List<BuildTargetData> = emptyList(),
  generatorName: String? = null,
  isTestOnly: Boolean = false,
  tags: List<String> = emptyList(),
  workspaceName: String = "_main",
): BuildTarget =
  BuildTarget(
    info = BuildTargetInfo(
      key = key,
      kind = kind,
      generatorName = generatorName,
      isTestOnly = isTestOnly,
      tags = tags,
      workspaceName = workspaceName,
    ),
    dependencies = dependencies,
    sources = sources,
    resources = resources,
    data = data,
  )

fun BuildTarget.withKey(key: WorkspaceTargetKey): BuildTarget = copy(info = info.copy(key = key))

fun BuildTarget.withTags(tags: List<String>): BuildTarget = copy(info = info.copy(tags = tags))
