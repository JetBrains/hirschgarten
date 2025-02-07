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
package org.jetbrains.bsp.bazel.server.sync.sharding

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.commons.BazelStatus
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.model.FirstPhaseProject
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.sharding.WildcardTargetExpander.ExpandedTargetsResult
import org.jetbrains.bsp.bazel.workspacecontext.DEFAULT_TARGET_SHARD_SIZE
import org.jetbrains.bsp.bazel.workspacecontext.ShardingApproach
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import kotlin.math.min

/**
 * Max number of individual targets per Bazel build shard.
 */
private const val MAX_TARGET_SHARD_SIZE = 10000

/**
 * number of packages per bazel query shard
 */
const val PACKAGE_SHARD_SIZE = 500

/** Utility methods for sharding Bazel build invocations.  */
object BazelBuildTargetSharder {
  /** Expand wildcard target patterns and partition the resulting target list.  */
  fun expandAndShardTargets(
    pathResolver: BazelPathsResolver,
    bazelInfo: BazelInfo,
    featureFlags: FeatureFlags,
    targets: TargetsSpec,
    context: WorkspaceContext,
    bazelRunner: BazelRunner,
    cancelChecker: CancelChecker,
    bspClientLogger: BspClientLogger,
    firstPhaseProject: FirstPhaseProject?,
  ): ShardedTargetsResult {
    if (firstPhaseProject != null) {
      return ShardedTargetsResult(
        shardTargetsToBatches(firstPhaseProject.modules.keys.toList(), emptyList(), getTargetShardSize(context)),
        BazelStatus.SUCCESS,
      )
    }
    val includes = targets.values
    val excludes = targets.excludedValues
    val shardingApproach = getShardingApproach(context)
    return when (shardingApproach) {
      ShardingApproach.SHARD_ONLY ->
        ShardedTargetsResult(
          shardTargetsToBatches(includes, excludes, getTargetShardSize(context)),
          BazelStatus.SUCCESS,
        )

      ShardingApproach.QUERY_AND_SHARD -> {
        val singleTargets =
          WildcardTargetExpander.queryIndividualTargets(includes, excludes, bazelRunner, cancelChecker, context)
        ShardedTargetsResult(
          shardTargetsToBatches(singleTargets.singleTargets, emptyList(), getTargetShardSize(context)),
          singleTargets.buildResult,
        )
      }

      ShardingApproach.EXPAND_AND_SHARD -> {
        val expandedTargets =
          expandWildcardTargets(
            pathResolver,
            bazelInfo,
            featureFlags,
            includes,
            excludes,
            bazelRunner,
            cancelChecker,
            bspClientLogger,
            context,
          )
        if (expandedTargets.buildResult == BazelStatus.FATAL_ERROR) {
          ShardedTargetsResult(ShardedTargetList(emptyList()), expandedTargets.buildResult)
        } else {
          ShardedTargetsResult(
            shardTargetsToBatches(expandedTargets.singleTargets, emptyList(), getTargetShardSize(context)),
            expandedTargets.buildResult,
          )
        }
      }
    }
  }

  private fun getShardingApproach(context: WorkspaceContext): ShardingApproach =
    context.shardingApproachSpec.value ?: ShardingApproach.QUERY_AND_SHARD

  /** Number of individual targets per blaze build shard.  */
  private fun getTargetShardSize(context: WorkspaceContext): Int =
    min(context.targetShardSize.value ?: DEFAULT_TARGET_SHARD_SIZE, MAX_TARGET_SHARD_SIZE)

  /**
   *  Expand wildcard target patterns into individual bazel targets.
   */
  private fun expandWildcardTargets(
    pathsResolver: BazelPathsResolver,
    bazelInfo: BazelInfo,
    featureFlags: FeatureFlags,
    includes: List<Label>,
    excludes: List<Label>,
    bazelRunner: BazelRunner,
    cancelChecker: CancelChecker,
    bspClientLogger: BspClientLogger,
    context: WorkspaceContext,
  ): ExpandedTargetsResult {
    val wildcardIncludes = includes.filter { it.isWildcard }
    if (wildcardIncludes.isEmpty()) {
      return ExpandedTargetsResult(includes, BazelStatus.SUCCESS)
    }
    val expandedTargets: Map<Label, List<Label>> =
      WildcardTargetExpander.expandToNonRecursiveWildcardTargets(
        pathsResolver,
        bazelInfo,
        featureFlags,
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
          cancelChecker,
          bspClientLogger,
          context,
        ).orEmpty()

    // finally add back any explicitly-specified, unexcluded single targets which may have been
    // removed by the query (for example, because they have the 'manual' tag)
    val singleTargets = includes.filter { !it.isWildcard }
    return ExpandedTargetsResult.merge(
      result,
      ExpandedTargetsResult(singleTargets, result.buildResult),
    )
  }

  /**
   * Shards a list of individual blaze targets (with no wildcard expressions other than for excluded
   * target patterns).
   */
  private fun shardTargetsToBatches(
    targets: List<Label>,
    excludes: List<Label>,
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
