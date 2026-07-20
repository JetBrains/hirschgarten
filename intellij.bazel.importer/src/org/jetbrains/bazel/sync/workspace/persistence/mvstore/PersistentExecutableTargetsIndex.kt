package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndex

internal class PersistentExecutableTargetsIndex(
  val partialSnapshot: PersistentWorkspaceSnapshot,
  val generation: SnapshotGeneration,
) : ExecutableTargetsIndex {
  override fun executableTargetsFor(label: Label): List<Label> = generation.findExecutableTargets(partialSnapshot, label)
}
