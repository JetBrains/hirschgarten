package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class ShardSyncSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

private const val DEFAULT_SHARD_SYNC_VALUE = false

internal object ShardSyncSpecExtractor : ExecutionContextEntityExtractor<ShardSyncSpec> {
  override fun fromProjectView(projectView: ProjectView): ShardSyncSpec =
    ShardSyncSpec(projectView.shardSync?.value ?: DEFAULT_SHARD_SYNC_VALUE)
}
