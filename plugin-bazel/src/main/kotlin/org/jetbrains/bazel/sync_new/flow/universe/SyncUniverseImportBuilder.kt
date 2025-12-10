package org.jetbrains.bazel.sync_new.flow.universe

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge

object SyncUniverseImportBuilder {
  suspend fun createUniverseImport(project: Project): SyncUniverseImportState {
    val workspaceContext = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    workspaceContext.directories
    val patterns = workspaceContext.targets.toFilteredCondition()
    val directories = workspaceContext.directories.toFilteredCondition()
    val internalRepos = patterns.filterIsInstance<SyncUniverseFilteredCondition.Include<Label>>()
      .map { it.element.assumeResolved() }
      .map { it.repo.repoName }
      .toSet()
    return SyncUniverseImportState(
      patterns = patterns.toSet(),
      directories = directories.toSet(),
      internalRepos = internalRepos,
    )
  }

  private fun <T> List<ExcludableValue<T>>.toFilteredCondition(): List<SyncUniverseFilteredCondition<T>> = this.map {
    when (it) {
      is ExcludableValue.Excluded<T> -> SyncUniverseFilteredCondition.Exclude(it.value)
      is ExcludableValue.Included<T> -> SyncUniverseFilteredCondition.Include(it.value)
    }
  }
}
