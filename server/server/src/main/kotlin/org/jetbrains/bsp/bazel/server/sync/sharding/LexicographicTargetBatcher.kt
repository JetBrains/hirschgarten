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
package org.jetbrains.bsp.bazel.server.sync.sharding

import org.jetbrains.bsp.bazel.server.model.Label

/**
 * A simple target batcher splitting based on the target strings. This will tend to split by
 * package, so it is better than random batching.
 */
class LexicographicTargetBatcher : BuildBatchingService {
  override fun calculateTargetBatches(targets: Set<Label>, suggestedShardSize: Int): List<List<Label>> {
    val sorted = targets.sortedBy { label -> label.value }
    return sorted.chunked(suggestedShardSize)
  }

  companion object {
    // The maximum number of targets per shard for remote builds to avoid potential OOM
    val maximumRemoteShardSize = 1000

    // The minimum number of targets per shard for remote builds. If the user explicitly
    // sets a smaller target_shard_size, the user-specified value takes priority.
    val minimumRemoteShardSize = 500

    // The minimum targets size requirement to use all idle workers. Splitting targets does not help
    // to reduce build time when their target size is too small. So set a threshold to avoid
    // over-split.
    val parallelThreshold = 1000

    // If true, ensures that builds with remote sharding use at least LEGACY_CONCURRENT_SHARD_COUNT
    // concurrent shards. This ensures that remote builds do not change for mid-sized projects from
    // before there was an explicit minimum value for targets per remote shard.
    // Ignored if LEGACY_CONCURRENT_SHARD_COUNT is greater than the total number of concurrent build
    // shards.
    @com.google.common.annotations.VisibleForTesting
    val useLegacySharding = true

    private const val LEGACY_CONCURRENT_SHARD_COUNT = 10

    /**
     * Calculates the number of targets to run on a single build shard along.
     *
     *
     * If the number of targets to shard meets the threshold for sharding, then the value is
     *
     *
     * clamp(min targets per shard, (# targets) / (# shards running in parallel), max targets per
     * shard)
     *
     *
     * If [LexicographicTargetBatcher.useLegacySharding] is enabled, projects surpassing the
     * splitting threshold are split across at least [ ][LexicographicTargetBatcher.LEGACY_CONCURRENT_SHARD_COUNT] shards, even if the individual shard
     * size is smaller than [LexicographicTargetBatcher.minimumRemoteShardSize].
     *
     * @param numTargets The number of targets to split into shards.
     * @param parallelThreshold The minimum number of targets required for splitting a build into
     * shards. [LexicographicTargetBatcher.parallelThreshold]
     * @param numConcurrentShards The maximum number of shards that can run in parallel. [     ][ShardedTargetList.remoteConcurrentSyncs]
     * @param min The minimum number of targets per remote build shard. [     ][LexicographicTargetBatcher.minimumRemoteShardSize]
     * @param max The maximum number of targets per remote build shard. [     ][LexicographicTargetBatcher.maximumRemoteShardSize]
     * @param suggested The target_shard_size, if specified in the blazeproject file. Otherwise [     ][BlazeBuildTargetSharder.defaultTargetShardSize] Takes precedence if smaller than min.
     * @return The number of targets per build shard.
     */
    fun computeParallelShardSize(
      numTargets: Int,
      parallelThreshold: Int,
      numConcurrentShards: Int,
      min: Int,
      max: Int,
      suggested: Int,
    ): Int {
      if (numTargets < parallelThreshold) {
        return suggested
      }
      var targetsPerShard: Int
      if (useLegacySharding &&
        LEGACY_CONCURRENT_SHARD_COUNT <= numConcurrentShards &&
        numTargets < min * LEGACY_CONCURRENT_SHARD_COUNT
      ) {
        targetsPerShard =
          kotlin.math
            .ceil(numTargets.toDouble() / LEGACY_CONCURRENT_SHARD_COUNT)
            .toInt()
      } else {
        targetsPerShard = kotlin.math.ceil(numTargets.toDouble() / numConcurrentShards).toInt()
        targetsPerShard =
          com.google.common.primitives.Ints
            .constrainToRange(targetsPerShard, min, max)
      }
      return kotlin.math.min(suggested.toDouble(), targetsPerShard.toDouble()).toInt()
    }
  }
}
