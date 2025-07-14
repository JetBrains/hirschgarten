package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.TargetShardSizeSpec

const val DEFAULT_TARGET_SHARD_SIZE = 1000

internal object TargetShardSizeSpecExtractor : WorkspaceContextEntityExtractor<TargetShardSizeSpec> {
  override fun fromProjectView(projectView: ProjectView): TargetShardSizeSpec =
    TargetShardSizeSpec(projectView.targetShardSize?.value ?: DEFAULT_TARGET_SHARD_SIZE)
}
