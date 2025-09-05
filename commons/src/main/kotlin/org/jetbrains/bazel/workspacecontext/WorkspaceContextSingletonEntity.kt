package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

/**
 * Base single-value `WorkspaceContext` entity class - you need to extend it if you want
 * to create your single-value entity.
 */
abstract class WorkspaceContextSingletonEntity<T> : WorkspaceContextEntity() {
  abstract val value: T
}

data class AllowManualTargetsSyncSpec(override val value: Boolean) : WorkspaceContextSingletonEntity<Boolean>()

data class BazelBinarySpec(override val value: Path) : WorkspaceContextSingletonEntity<Path>()

data class DotBazelBspDirPathSpec(override val value: Path) : WorkspaceContextSingletonEntity<Path>()

data class GazelleTargetSpec(override val value: Label?) : WorkspaceContextSingletonEntity<Label?>()

data class IdeJavaHomeOverrideSpec(override val value: Path?) : WorkspaceContextSingletonEntity<Path?>()

data class ImportDepthSpec(override val value: Int) : WorkspaceContextSingletonEntity<Int>()

data class ImportIjarsSpec(override val value: Boolean) : WorkspaceContextSingletonEntity<Boolean>()

data class IndexAllFilesInDirectoriesSpec(override val value: Boolean) : WorkspaceContextSingletonEntity<Boolean>()

data class DeriveInstrumentationFilterFromTargetsSpec(override val value: Boolean) : WorkspaceContextSingletonEntity<Boolean>()

enum class ShardingApproach {
  EXPAND_AND_SHARD, // expand wildcard targets to package targets, query single targets, and then shard to batches
  QUERY_AND_SHARD, // query single targets from the given list of targets, and then shard to batches
  SHARD_ONLY, // split unexpanded wildcard targets into batches
  ;

  companion object {
    fun fromString(rawValue: String?): ShardingApproach? = ShardingApproach.entries.find { it.name.equals(rawValue, ignoreCase = true) }
  }
}

data class ShardingApproachSpec(override val value: ShardingApproach?) : WorkspaceContextSingletonEntity<ShardingApproach?>()

data class ShardSyncSpec(override val value: Boolean) : WorkspaceContextSingletonEntity<Boolean>()

data class TargetShardSizeSpec(override val value: Int) : WorkspaceContextSingletonEntity<Int?>()
