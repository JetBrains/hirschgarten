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

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.ShardingApproach
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
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
@ApiStatus.Internal
object BazelBuildTargetSharder {
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
          ShardedTargetsResult(emptyList(), expandedTargets.buildResult)
        } else {
          ShardedTargetsResult(
            shardTargetsToBatches(expandedTargets.singleTargets, excludes, getTargetShardSize(projectView)),
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

    // finally add back any explicitly-specified single targets which may have been removed by the
    // query (for example, because they have the 'manual' tag). shardTargetsToBatches drops the
    // excluded ones again.
    val singleTargets = includes.filterTo(LinkedHashSet()) { !it.isWildcard }
    return ExpandedTargetsResult.merge(
      result,
      ExpandedTargetsResult(singleTargets, result.buildResult),
    )
  }

  /**
   * Shards a list of individual Bazel targets (with no wildcard expressions other than for excluded
   * target patterns).
   *
   * An exact excluded label is removed here. An excluded wildcard pattern goes to every shard, and
   * Bazel applies it, because a [Label] cannot match a pattern.
   */
  fun shardTargetsToBatches(
    targets: Collection<Label>,
    excludes: Collection<Label>,
    softShardSize: Int,
  ): List<TargetCollection> {
    val exactExcludes = excludes.filterNotTo(HashSet()) { it.isWildcard }
    val targetBatches = calculateTargetBatches(targets - exactExcludes, softShardSize)
    return targetBatches.map { batch ->
      TargetCollection(
        values = batch,
        excludedValues = excludes.toList(),
      )
    }
  }

  /**
   * Given a list of individual, un-excluded blaze targets (no wildcard target patterns), returns a
   * list of target batches.
   *
   * Two rules apply:
   * - A batch holds the targets of one repository only. Bazel loads a repository per batch, so a
   *   mixed batch makes the build load more than it needs.
   * - A batch keeps the targets of one package together. Targets in a package share their load and
   *   analysis work.
   *
   * Therefore [softShardSize] is a goal, not a limit. A batch takes one more package only while it
   * stays at or below the goal, but a single package larger than the goal still goes into one batch.
   * [MAX_TARGET_SHARD_SIZE] is the true limit. A package above it is split, because a batch that
   * large risks an OOM.
   */
  fun calculateTargetBatches(targets: Collection<Label>, softShardSize: Int): List<List<Label>> {
    require(softShardSize > 0) { "The shard size must be greater than zero, but it is $softShardSize" }
    return targets
      .sorted()
      .groupBy { label -> (label as? ResolvedLabel)?.repo }
      .values
      .flatMap { repositoryTargets -> batchPackagesOfOneRepository(repositoryTargets, softShardSize) }
  }

  /**
   * Puts the packages of one repository into batches. Keeps the targets of a package together, and
   * fills a batch up to [softShardSize].
   */
  private fun batchPackagesOfOneRepository(targets: List<Label>, softShardSize: Int): List<List<Label>> {
    val batches = mutableListOf<List<Label>>()
    var batch = mutableListOf<Label>()
    for (packageTargets in targets.groupBy { label -> label.packagePath }.values) {
      val splitPackage = packageTargets.size > MAX_TARGET_SHARD_SIZE
      val batchIsFull = batch.size + packageTargets.size > softShardSize
      if (batch.isNotEmpty() && (splitPackage || batchIsFull)) {
        batches.add(batch)
        batch = mutableListOf()
      }
      if (splitPackage) {
        batches.addAll(packageTargets.chunked(MAX_TARGET_SHARD_SIZE))
      } else {
        batch.addAll(packageTargets)
      }
    }
    if (batch.isNotEmpty()) {
      batches.add(batch)
    }
    return batches
  }

  /**
   * Partition targets list, retaining the original relative ordering. The caller passes the excluded
   * targets separately.
   */
  fun shardTargetsRetainingOrdering(targets: List<Label>, shardSize: Int): List<List<Label>> = targets.chunked(shardSize)

  /** Result of expanding then sharding wildcard target patterns  */
  data class ShardedTargetsResult(val targets: List<TargetCollection>, val buildResult: BazelStatus)
}
