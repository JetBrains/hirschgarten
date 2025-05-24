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
 * A simple target batcher splitting based on the target strings. This will tend to split by
 * package, so it is better than random batching.
 */
class LexicographicTargetBatcher : BuildBatchingService {
  override fun calculateTargetBatches(
    targets: Set<Label>,
    excludes: Set<TargetPattern>,
    suggestedShardSize: Int,
  ): List<List<Label>> {
    val sorted = targets.sortedBy { label -> label.toString() }
    // TODO: excludes are not used here
    return sorted.chunked(suggestedShardSize)
  }
}
