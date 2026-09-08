package org.jetbrains.bazel.assertions

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget

internal suspend fun Project.findTargets(label: Label): List<BuildTarget> {
  val snapshot = service<WorkspaceSnapshotService>().currentSnapshot()

  return snapshot.targetGraph.allTargets
    .filter { it.label == label }
    .mapNotNull { snapshot.targets.findTargetByKey(it, TargetLoadOptions.ALL) }
    .toList()
}

internal suspend fun Project.findTarget(label: Label): BuildTarget {
  val targets = findTargets(label)
  assertThat(targets).hasSize(1)

  return targets.single()
}

internal suspend fun Project.findTargets(label: String): List<BuildTarget> {
  return findTargets(Label.parse(label))
}

internal suspend fun Project.findTarget(label: String): BuildTarget {
  val targets = findTargets(label)
  assertThat(targets).hasSize(1)

  return targets.single()
}

internal suspend fun Project.findTarget(key: WorkspaceTargetKey): BuildTarget {
  val snapshot = service<WorkspaceSnapshotService>().currentSnapshot()
  return snapshot.targets.findTargetByKey(key, TargetLoadOptions.ALL).assertNotNull()
}
