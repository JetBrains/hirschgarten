package org.jetbrains.bazel.ui.gutters

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocationCollection

@ApiStatus.Internal
data class NonImportedBuildTarget(
  override val key: WorkspaceTargetKey,
  override val kind: TargetKind,
  override val tags: List<String> = emptyList()
) : BuildTarget {
  constructor(
    label: Label,
    kind: TargetKind,
    tags: List<String> = emptyList(),
  ) : this(WorkspaceTargetKey(label = label), kind, tags)

  override val loaded: TargetLoadOptions get() = TargetLoadOptions.MINIMAL

  override val generatorName: String? get() = null
  override val isTestOnly: Boolean get() = false

  override val dependencies: List<DependencyLabel> get() = listOf()

  override val sources: OutputLocationCollection get() = OutputLocationCollection.EMPTY
  override val resources: OutputLocationCollection get() = OutputLocationCollection.EMPTY

  override val data: List<BuildTargetData> get() = listOf()
}
