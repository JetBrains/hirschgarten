/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.TargetPattern

/**
 * A service for splitting up a large set of targets into batches, built one at a time.
 *
 *
 * The goal is primarily to avoid OOMEs, with a secondary goal of reducing build latency.
 */
interface BuildBatchingService {
  /**
   * Given a list of individual, un-excluded blaze targets (no wildcard target patterns), returns a
   * list of target batches.
   *
   *
   * Individual implementations may use different criteria for this batching, with the general
   * goal of avoiding OOMEs.
   *
   * @param suggestedShardSize a suggestion only; may be entirely ignored by the implementation
   */
  fun calculateTargetBatches(
    targets: Set<Label>,
    excludes: Set<TargetPattern>,
    suggestedShardSize: Int,
  ): List<List<Label>>

  /**
   * Given a list of individual, un-excluded bazel targets (no wildcard target patterns), create
   * ShardedTargetList according to inputs.
   */
  fun getShardedTargetList(
    targets: Set<Label>,
    excludes: Set<TargetPattern>,
    suggestedShardSize: Int,
  ): ShardedTargetList {
    val targetBatches = calculateTargetBatches(targets, excludes, suggestedShardSize)
    return ShardedTargetList(targetBatches)
  }
}
