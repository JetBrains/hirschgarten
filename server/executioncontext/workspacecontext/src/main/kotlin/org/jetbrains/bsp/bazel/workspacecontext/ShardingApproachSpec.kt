package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity

data class ShardingApproachSpec(override val value: ShardingApproach?) : ExecutionContextSingletonEntity<ShardingApproach?>()

enum class ShardingApproach {
  EXPAND_AND_SHARD, // expand wildcard targets to package targets, query single targets, and then shard to batches
  QUERY_AND_SHARD, // query single targets from the given list of targets, and then shard to batches
  SHARD_ONLY, // split unexpanded wildcard targets into batches
  ;

  companion object {
    fun fromString(rawValue: String?): ShardingApproach? = ShardingApproach.entries.find { it.name.equals(rawValue, ignoreCase = true) }
  }
}

internal object ShardingApproachSpecExtractor : ExecutionContextEntityExtractor<ShardingApproachSpec> {
  override fun fromProjectView(projectView: org.jetbrains.bsp.bazel.projectview.model.ProjectView): ShardingApproachSpec =
    ShardingApproachSpec(ShardingApproach.fromString(projectView.shardingApproach?.value))
}
