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
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.sharding.WildcardTargetExpander.ExpandedTargetsResult
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
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
  fun expandToSingleTargets(
    packageTargets: List<Label>,
    excludes: List<Label>,
    bazelRunner: BazelRunner,
    cancelChecker: CancelChecker,
    bspClientLogger: BspClientLogger,
    context: WorkspaceContext,
  ): ExpandedTargetsResult? {
    val shards =
      BazelBuildTargetSharder.shardTargetsRetainingOrdering(
        packageTargets,
        PACKAGE_SHARD_SIZE,
      )
    var output: ExpandedTargetsResult? = null
    for (shard in shards) {
      val result: ExpandedTargetsResult =
        queryIndividualTargets(
          shard,
          excludes,
          bazelRunner,
          cancelChecker,
          context,
        )
      output =
        if (output == null) {
          result
        } else {
          ExpandedTargetsResult.merge(
            output,
            result,
          )
        }
      if (output.buildResult == BazelStatus.FATAL_ERROR) {
        bspClientLogger.warn("Bazel query for expanding package targets failed with fatal error. Skipping further expanding queries.")
        return output
      }
    }
    return output
  }

  /** Runs a Bazel query to expand the input target patterns to individual Bazel targets.  */
  fun queryIndividualTargets(
    includedPatterns: List<Label>,
    excludedTargets: List<Label>,
    bazelRunner: BazelRunner,
    cancelChecker: CancelChecker,
    context: WorkspaceContext,
  ): ExpandedTargetsResult {
    val targetsSpec =
      TargetsSpec(
        values = includedPatterns,
        excludedValues = excludedTargets,
      )
    val command =
      bazelRunner.buildBazelCommand {
        // exclude 'manual' targets,
        // which shouldn't be built when expanding wildcard target patterns if `allow_manual_targets_sync` is not specified.
        query(allowManualTargetsSync = context.allowManualTargetsSync.value) {
          addTargetsFromSpec(targetsSpec)
          options.addAll(listOf("--output=label", "--keep_going"))
        }
      }
    val queryResult =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null, shouldLogInvocation = false)
        .waitAndGetResult(cancelChecker, ensureAllOutputRead = true)
    return ExpandedTargetsResult(
      singleTargets = queryResult.stdoutLines.map { Label.parse(it) },
      queryResult.bazelStatus,
    )
  }

  class ExpandedTargetsResult(val singleTargets: List<Label>, val buildResult: BazelStatus) {
    companion object {
      fun merge(first: ExpandedTargetsResult, second: ExpandedTargetsResult): ExpandedTargetsResult {
        val buildResult: BazelStatus = first.buildResult.merge(second.buildResult)
        val targets = (first.singleTargets + second.singleTargets).distinct()
        return ExpandedTargetsResult(
          targets,
          buildResult,
        )
      }
    }
  }
}

internal fun ExpandedTargetsResult?.orEmpty() = this ?: ExpandedTargetsResult(emptyList(), BazelStatus.SUCCESS)
