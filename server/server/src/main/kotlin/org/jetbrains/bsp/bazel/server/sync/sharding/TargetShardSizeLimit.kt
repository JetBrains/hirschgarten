// Copyright 2019-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
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

/** An extension point for limiting the number of targets per Bazel build shard.  */
interface TargetShardSizeLimit {
  /**
   * Returns the maximum number of blaze targets per shard for this limit.
   *
   * If `null`, this limit is not enforced.
   */
  fun getShardSizeLimit(): Int?
}
