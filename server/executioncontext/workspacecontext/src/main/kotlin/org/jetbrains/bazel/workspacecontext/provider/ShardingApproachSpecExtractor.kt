package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.ShardingApproach
import org.jetbrains.bazel.workspacecontext.ShardingApproachSpec

internal object ShardingApproachSpecExtractor : ExecutionContextEntityExtractor<ShardingApproachSpec> {
  override fun fromProjectView(projectView: ProjectView): ShardingApproachSpec =
    ShardingApproachSpec(ShardingApproach.Companion.fromString(projectView.shardingApproach?.value))
}
