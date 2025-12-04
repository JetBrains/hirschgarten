package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.SyncScope

data class SyncExpandContext(
  val project: Project,
  val service: SyncExpandService,
  val scope: SyncScope
)
