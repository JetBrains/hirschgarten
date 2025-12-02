package org.jetbrains.bazel.sync_new.flow.hash_diff

import com.intellij.openapi.project.Project

data class SyncHasherContext(
  val project: Project,
  val service: SyncHasherService
)
