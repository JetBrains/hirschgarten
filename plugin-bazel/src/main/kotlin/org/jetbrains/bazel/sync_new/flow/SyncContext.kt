package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project

data class SyncContext(
  val project: Project,
  val strategy: SyncScope
)
