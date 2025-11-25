package org.jetbrains.bazel.sync_new.flow.diff.query

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseTargetPattern

// TODO: use QueryBuilder
// TODO: what it has to be here
object QueryTargetPattern {
  // get all targets in universe
  fun createUniverseQuery(patterns: List<SyncUniverseTargetPattern>, includeDeps: Boolean = true): String {
    val expr = buildString {
      val includes = patterns.filterIsInstance<SyncUniverseTargetPattern.Include>()
        .joinToString(separator = " + ") { it.label.toString() }
      val excludes = patterns.filterIsInstance<SyncUniverseTargetPattern.Exclude>()
        .joinToString(separator = " + ") { it.label.toString() }
      if (includes.isEmpty()) {
        append("//...:all")
      } else {
        append(includes)
      }
      if (excludes.isNotEmpty()) {
        append(" - ")
        append("(")
        append(excludes)
        append(")")
      }
    }
    return if (includeDeps) {
      "deps($expr)"
    } else {
      expr
    }
  }

  suspend fun getProjectTargetUniverse(project: Project): List<SyncUniverseTargetPattern> {
    val workspaceContext = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
    return workspaceContext.targets.map {
      when (it) {
        is ExcludableValue.Excluded<Label> -> SyncUniverseTargetPattern.Exclude(it.value)
        is ExcludableValue.Included<Label> -> SyncUniverseTargetPattern.Include(it.value)
      }
    }
  }
}
