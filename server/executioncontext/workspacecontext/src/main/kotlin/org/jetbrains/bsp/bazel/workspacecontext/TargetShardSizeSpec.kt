package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class TargetShardSizeSpec(override val value: Int?) : ExecutionContextSingletonEntity<Int?>()

const val DEFAULT_TARGET_SHARD_SIZE = 1000

internal object TargetShardSizeSpecExtractor : ExecutionContextEntityExtractor<TargetShardSizeSpec> {
  override fun fromProjectView(projectView: ProjectView): TargetShardSizeSpec =
    TargetShardSizeSpec(projectView.targetShardSize?.value ?: DEFAULT_TARGET_SHARD_SIZE)
}
