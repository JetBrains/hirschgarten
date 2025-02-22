package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class ShardSyncSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

/**
 * Due to Bazel memory leak issue, it is not recommended to enable shard sync by default.
 *
 * Check out [this issue](https://github.com/bazelbuild/bazel/issues/19412) for more info.
 */
private const val DEFAULT_SHARD_SYNC_VALUE = false

internal object ShardSyncSpecExtractor : ExecutionContextEntityExtractor<ShardSyncSpec> {
  override fun fromProjectView(projectView: ProjectView): ShardSyncSpec =
    ShardSyncSpec(projectView.shardSync?.value ?: DEFAULT_SHARD_SYNC_VALUE)
}
