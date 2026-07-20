package org.jetbrains.bazel.target

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.ExecutableTarget
import java.nio.file.Path

@ApiStatus.Internal
data class TargetSummary(
  val key: WorkspaceTargetKey,
  override val kind: TargetKind,
  val baseDirectory: Path,
  val isManual: Boolean,
  val isWorkspace: Boolean,
) : ExecutableTarget {
  override val id: Label
    get() = key.label

  val label: Label
    get() = key.label
}
