package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph

data class SyncContext(
  val project: Project,
  val scope: SyncScope,
  val graph: BazelTargetGraph,
  val syncExecutor: SyncExecutor
)
