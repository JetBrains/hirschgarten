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
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.sync.sharding.WildcardTargetExpander.ExpandedTargetsResult
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelTaskLogger
import org.jetbrains.bsp.protocol.FeatureFlags

/** Expands wildcard target patterns into individual Bazel targets.  */
object WildcardTargetExpander {
  /**
   * Expand recursive wildcard Bazel target patterns into single-package wildcard patterns, via a
   * file system traversal.
   */
  fun expandToNonRecursiveWildcardTargets(
    pathResolver: BazelPathsResolver,
    bazelInfo: BazelInfo,
    featureFlags: FeatureFlags,
    wildcardIncludes: List<Label>,
  ): Map<Label, List<Label>> =
    PackageLister.expandPackageTargets(
      pathResolver,
      bazelInfo,
      featureFlags,
      wildcardIncludes,
    )

  /** Runs a sharded Bazel query to expand wildcard targets to individual Bazel targets  */
  suspend fun expandToSingleTargets(
    packageTargets: List<Label>,
    excludes: List<Label>,
    bazelRunner: BazelRunner,
    taskLogger: BazelTaskLogger,
    context: WorkspaceContext,
  ): ExpandedTargetsResult? {
    val shards =
      BazelBuildTargetSharder.shardTargetsRetainingOrdering(
        packageTargets,
        PACKAGE_SHARD_SIZE,
      )
    if (shards.isEmpty()) return null

    val singleTargets = mutableSetOf<Label>()
    var buildResult = BazelStatus.SUCCESS
    for (shard in shards) {
      val result = queryIndividualTargets(shard, excludes, bazelRunner, context)
      singleTargets.addAll(result.singleTargets)
      buildResult = buildResult.merge(result.buildResult)
      if (buildResult == BazelStatus.FATAL_ERROR) {
        taskLogger.warn("Bazel query for expanding package targets failed with fatal error. Skipping further expanding queries.")
        return ExpandedTargetsResult(singleTargets, buildResult)
      }
    }
    return ExpandedTargetsResult(singleTargets, buildResult)
  }

  /** Runs a Bazel query to expand the input target patterns to individual Bazel targets.  */
  suspend fun queryIndividualTargets(
    includedPatterns: List<Label>,
    excludedTargets: List<Label>,
    bazelRunner: BazelRunner,
    context: WorkspaceContext,
  ): ExpandedTargetsResult {
    val targetsSpec =
      TargetCollection(
        values = includedPatterns,
        excludedValues = excludedTargets,
      )
    val command =
      bazelRunner.buildBazelCommand(context) {
        // exclude 'manual' targets,
        // which shouldn't be built when expanding wildcard target patterns if `allow_manual_targets_sync` is not specified.
        query(allowManualTargetsSync = context.allowManualTargetsSync) {
          this.targets.addAll(targetsSpec.values)
          this.excludedTargets.addAll(targetsSpec.excludedValues)
          options.addAll(listOf("--output=label", "--keep_going"))
        }
      }
    val queryResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, shouldLogInvocation = false)
        .waitAndGetResult()
    return ExpandedTargetsResult(
      singleTargets = queryResult.stdoutLines.mapTo(LinkedHashSet()) { Label.parse(it) },
      queryResult.bazelStatus,
    )
  }

  class ExpandedTargetsResult(val singleTargets: Set<Label>, val buildResult: BazelStatus) {
    companion object {
      fun merge(first: ExpandedTargetsResult, second: ExpandedTargetsResult): ExpandedTargetsResult {
        val buildResult: BazelStatus = first.buildResult.merge(second.buildResult)
        val targets = LinkedHashSet(first.singleTargets).apply { addAll(second.singleTargets) }
        return ExpandedTargetsResult(targets, buildResult)
      }
    }
  }
}

internal fun ExpandedTargetsResult?.orEmpty() = this ?: ExpandedTargetsResult(emptySet(), BazelStatus.SUCCESS)
