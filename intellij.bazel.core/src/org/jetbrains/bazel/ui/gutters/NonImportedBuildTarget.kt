package org.jetbrains.bazel.ui.gutters

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path

@ApiStatus.Internal
data class NonImportedBuildTarget(
  override val key: WorkspaceTargetKey,
  override val kind: TargetKind,
  override val baseDirectory: Path,
  override val tags: List<String> = emptyList()
) : BuildTarget {
  constructor(
    label: Label,
    kind: TargetKind,
    baseDirectory: Path,
    tags: List<String> = emptyList(),
  ) : this(WorkspaceTargetKey(label = label), kind, baseDirectory, tags)

  override val loaded: TargetLoadOptions get() = TargetLoadOptions.MINIMAL

  override val generatorName: String? get() = null
  override val isWorkspace: Boolean get() = true
  override val isTestOnly: Boolean get() = false

  override val dependencies: List<DependencyLabel> get() = listOf()

  override val sources: SourceFileCollection get() = SourceFileCollection.EMPTY
  override val generatedSources: SourceFileCollection get() = SourceFileCollection.EMPTY
  override val resources: SourceFileCollection get() = SourceFileCollection.EMPTY

  override val data: List<BuildTargetData> get() = listOf()
}
