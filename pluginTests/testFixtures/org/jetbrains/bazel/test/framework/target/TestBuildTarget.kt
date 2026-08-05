package org.jetbrains.bazel.test.framework.target

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.JavaLanguageClass
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.io.path.Path

data class TestBuildTarget(
  override val key: WorkspaceTargetKey = WorkspaceTargetKey(label = Label.parse("//target")),
  override val dependencies: List<DependencyLabel> = emptyList(),
  override val kind: TargetKind = TargetKind(
    kind = "java_library",
    ruleType = RuleType.LIBRARY,
    languageClasses = setOf(JavaLanguageClass.JAVA),
  ),
  override val sources: SourceFileCollection = SourceFileCollection.EMPTY,
  override val generatedSources: SourceFileCollection = SourceFileCollection.EMPTY,
  override val resources: SourceFileCollection = SourceFileCollection.EMPTY,
  override val baseDirectory: Path = Path("/base/dir"),
  override val data: List<BuildTargetData> = emptyList(),
  override val generatorName: String? = null,
  override val isManual: Boolean = false,
  override val isWorkspace: Boolean = true,
  override val isTestOnly: Boolean = false,
  override val tags: List<String> = emptyList(),
  override val loaded: TargetLoadOptions = TargetLoadOptions.ALL,
) : BuildTarget

// needed for test, so you can do `==` on `BuildTarget`
fun BuildTarget.asTestBuildTarget(): TestBuildTarget =
  TestBuildTarget(
    key = key,
    dependencies = dependencies,
    kind = kind,
    sources = sources,
    generatedSources = generatedSources,
    resources = resources,
    baseDirectory = baseDirectory,
    data = data,
    generatorName = generatorName,
    isManual = isManual,
    isWorkspace = isWorkspace,
    isTestOnly = isTestOnly,
    tags = tags,
    loaded = loaded,
  )
