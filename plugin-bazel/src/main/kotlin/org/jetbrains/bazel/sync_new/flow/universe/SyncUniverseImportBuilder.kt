package org.jetbrains.bazel.sync_new.flow.universe

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge

object SyncUniverseImportBuilder {
  suspend fun createUniverseImport(project: Project): SyncUniverseImportState {
    val workspaceContext = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    val patterns = workspaceContext.targets.map {
      when (it) {
        is ExcludableValue.Excluded<Label> -> SyncUniverseTargetPattern.Exclude(it.value)
        is ExcludableValue.Included<Label> -> SyncUniverseTargetPattern.Include(it.value)
      }
    }
    val internalRepos = patterns.filterIsInstance<SyncUniverseTargetPattern.Include>()
      .map { it.label.assumeResolved() }
      .map { it.repo.repoName }
      .toSet()
    return SyncUniverseImportState(
      patterns = patterns.toSet(),
      internalRepos = internalRepos,
    )
  }
}
