/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.server.sync.sharding

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.ShardingApproach
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.shardingApproach
import org.jetbrains.bazel.languages.projectview.targetShardSize
import org.jetbrains.bazel.server.sync.sharding.WildcardTargetExpander.ExpandedTargetsResult
import org.jetbrains.bsp.protocol.BazelTaskLogger
import kotlin.math.min

/**
 * Max number of individual targets per Bazel build shard.
 */
private const val MAX_TARGET_SHARD_SIZE = 10000

/**
 * number of packages per bazel query shard
 */
internal const val PACKAGE_SHARD_SIZE = 500

/** Utility methods for sharding Bazel build invocations.  */
internal object BazelBuildTargetSharder {
  /** Expand wildcard target patterns and partition the resulting target list.  */
  suspend fun expandAndShardTargets(
    pathResolver: BazelPathsResolver,
    targets: TargetCollection,
    projectView: ProjectView,
    bazelRunner: BazelRunner,
    taskLogger: BazelTaskLogger,
    allTargets: List<Label>?, /* all known targets, if any, from first phase */
  ): ShardedTargetsResult {
    if (allTargets != null) {
      return ShardedTargetsResult(
        shardTargetsToBatches(allTargets, emptyList(), getTargetShardSize(projectView)),
        BazelStatus.SUCCESS,
      )
    }
    val includes = targets.values
    val excludes = targets.excludedValues
    val shardingApproach = getShardingApproach(projectView)
    return when (shardingApproach) {
      ShardingApproach.SHARD_ONLY ->
        ShardedTargetsResult(
          shardTargetsToBatches(includes, excludes, getTargetShardSize(projectView)),
          BazelStatus.SUCCESS,
        )

      ShardingApproach.QUERY_AND_SHARD -> {
        val singleTargets =
          WildcardTargetExpander.queryIndividualTargets(includes, excludes, bazelRunner, projectView)
        ShardedTargetsResult(
          shardTargetsToBatches(singleTargets.singleTargets, emptyList(), getTargetShardSize(projectView)),
          singleTargets.buildResult,
        )
      }

      ShardingApproach.EXPAND_AND_SHARD -> {
        val expandedTargets =
          expandWildcardTargets(
            pathResolver,
            includes,
            excludes,
            bazelRunner,
            taskLogger,
            projectView,
          )
        if (expandedTargets.buildResult == BazelStatus.FATAL_ERROR) {
          ShardedTargetsResult(ShardedTargetList(emptyList()), expandedTargets.buildResult)
        } else {
          ShardedTargetsResult(
            shardTargetsToBatches(expandedTargets.singleTargets, emptyList(), getTargetShardSize(projectView)),
            expandedTargets.buildResult,
          )
        }
      }
    }
  }

  private fun getShardingApproach(projectView: ProjectView): ShardingApproach =
    projectView.shardingApproach?.let {
      ShardingApproach.fromString(
        it,
      ) ?: ShardingApproach.QUERY_AND_SHARD
    } ?: ShardingApproach.QUERY_AND_SHARD

  /** Number of individual targets per blaze build shard.  */
  private fun getTargetShardSize(projectView: ProjectView): Int = min(projectView.targetShardSize, MAX_TARGET_SHARD_SIZE)

  /**
   *  Expand wildcard target patterns into individual bazel targets.
   */
  private suspend fun expandWildcardTargets(
    pathsResolver: BazelPathsResolver,
    includes: List<Label>,
    excludes: List<Label>,
    bazelRunner: BazelRunner,
    taskLogger: BazelTaskLogger,
    projectView: ProjectView,
  ): ExpandedTargetsResult {
    val wildcardIncludes = includes.filter { it.isWildcard }
    if (wildcardIncludes.isEmpty()) {
      return ExpandedTargetsResult(includes.toSet(), BazelStatus.SUCCESS)
    }
    val expandedTargets: Map<Label, List<Label>> =
      WildcardTargetExpander.expandToNonRecursiveWildcardTargets(
        pathsResolver,
        wildcardIncludes,
      )

    // replace original recursive targets with the expanded list, retaining relative ordering
    val fullList = arrayListOf<Label>()
    for (target in includes) {
      val expanded = expandedTargets[target]
      if (expanded == null) {
        fullList.add(target)
      } else {
        fullList.addAll(expanded)
      }
    }

    val result =
      WildcardTargetExpander
        .expandToSingleTargets(
          fullList,
          excludes,
          bazelRunner,
          taskLogger,
          projectView,
        ).orEmpty()

    // finally add back any explicitly-specified, unexcluded single targets which may have been
    // removed by the query (for example, because they have the 'manual' tag)
    val singleTargets = includes.filterTo(LinkedHashSet()) { !it.isWildcard }
    return ExpandedTargetsResult.merge(
      result,
      ExpandedTargetsResult(singleTargets, result.buildResult),
    )
  }

  /**
   * Shards a list of individual Bazel targets (with no wildcard expressions other than for excluded
   * target patterns).
   */
  private fun shardTargetsToBatches(
    targets: Collection<Label>,
    excludes: Collection<Label>,
    shardSize: Int,
  ): ShardedTargetList = LexicographicTargetBatcher().getShardedTargetList(targets.toSet(), excludes.toSet(), shardSize)

  /**
   * Partition targets list. Because order is important with respect to excluded targets, original
   * relative ordering is retained, and each shard has all subsequent excluded targets appended to
   * it.
   */
  fun shardTargetsRetainingOrdering(targets: List<Label>, shardSize: Int): List<List<Label>> = targets.chunked(shardSize)

  /** Result of expanding then sharding wildcard target patterns  */
  data class ShardedTargetsResult(val targets: ShardedTargetList, val buildResult: BazelStatus)
}
