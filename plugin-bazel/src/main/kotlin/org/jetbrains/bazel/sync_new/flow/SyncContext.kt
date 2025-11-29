package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.graph.impl.BazelFastTargetGraph
import org.jetbrains.bazel.sync_new.lang.SyncLanguageService

data class SyncContext(
  val project: Project,
  val scope: SyncScope,
  val graph: BazelFastTargetGraph,
  val syncExecutor: SyncExecutor,
  val languageService: SyncLanguageService,
)
