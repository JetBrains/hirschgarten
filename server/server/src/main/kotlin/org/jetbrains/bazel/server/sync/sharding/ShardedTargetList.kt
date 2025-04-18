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

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.TargetsSpec

/** Partitioned list of Bazel targets.  */
class ShardedTargetList(private val shardedTargets: List<List<Label>>, private val excludedTargets: List<Label> = listOf()) {
  fun toTargetsSpecs(): List<TargetsSpec> =
    shardedTargets.map { targets ->
      TargetsSpec(
        values = targets,
        excludedValues = excludedTargets,
      )
    }
}
