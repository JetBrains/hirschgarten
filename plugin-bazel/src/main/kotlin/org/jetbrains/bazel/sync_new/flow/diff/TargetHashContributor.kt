package org.jetbrains.bazel.sync_new.flow.diff

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseTargetPattern

interface TargetHashContributor {
  suspend fun computeHashes(project: Project, patterns: List<SyncUniverseTargetPattern>): Sequence<TargetHash>
}
