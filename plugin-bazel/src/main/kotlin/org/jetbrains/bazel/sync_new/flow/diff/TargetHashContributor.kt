package org.jetbrains.bazel.sync_new.flow.diff

import com.intellij.openapi.project.Project

interface TargetHashContributor {
  suspend fun computeHashes(project: Project, patterns: List<TargetPattern>): Sequence<TargetHash>
}
