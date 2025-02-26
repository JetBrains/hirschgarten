package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class TargetShardSizeSpec(override val value: Int) : ExecutionContextSingletonEntity<Int?>()

const val DEFAULT_TARGET_SHARD_SIZE = 1000

internal object TargetShardSizeSpecExtractor : ExecutionContextEntityExtractor<TargetShardSizeSpec> {
  override fun fromProjectView(projectView: ProjectView): TargetShardSizeSpec =
    TargetShardSizeSpec(projectView.targetShardSize?.value ?: DEFAULT_TARGET_SHARD_SIZE)
}
