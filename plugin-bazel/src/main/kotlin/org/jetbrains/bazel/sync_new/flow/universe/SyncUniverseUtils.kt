package org.jetbrains.bazel.sync_new.flow.universe

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.sync_new.flow.SyncRepoMapping

val Project.syncRepoMapping: SyncRepoMapping
  get() = service<SyncUniverseService>().universeState.get().repoMapping

// TODO: check if //... works
fun Label.isInsideUniverse(universe: SyncUniverseState): Boolean {
  return when (this) {
    is ResolvedLabel -> repo.repoName in universe.importState.internalRepos
      || repo.repoName.substringBefore("+") in universe.importState.internalRepos
    else -> false
  }
}
