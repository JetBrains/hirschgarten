package org.jetbrains.bazel.sync_new.flow.universe

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping

val Project.syncRepoMapping: SyncRepoMapping
  get() = service<SyncUniverseService>().universeState.get().repoMapping
