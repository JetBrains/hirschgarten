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

import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.workspacecontext.SystemInfo

/**
 * A {@link TargetShardSizeLimit} using heuristics to keep blaze invocations below system ARG_MAX.
 */
class ArgMaxShardSizeLimit(private val bspClientLogger: BspClientLogger) : TargetShardSizeLimit {
  private val systemArgMax: Int? by lazy(::queryArgMax)

  override fun getShardSizeLimit(): Int? {
    val argMax = systemArgMax ?: return null
    if (argMax <= 0) {
      return null
    }
    // a very rough heuristic with fixed, somewhat arbitrary env size and average target size
    val envSizeBytes = 20000
    val targetStringSizeBytes = 150
    return (argMax - envSizeBytes) / targetStringSizeBytes
  }

  /**
   * Synchronously runs `getconf ARG_MAX`, returning null if unsuccessful.
   *
   * Returns `32767` on windows which is the maximum length of lpCommandLine argument for the
   * [CreateProcessA](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessa)
   * function in the windows api.
   */
  private fun queryArgMax(): Int? {
    if (SystemInfo.isWindows) return 32767

    val process = ProcessBuilder("/usr/bin/getconf", "ARG_MAX").start()
    val retVal = process.waitFor()
    if (retVal != 0) {
      bspClientLogger.warn("Failed to query ARG_MAX: ${process.errorReader().readText()}")
      return null
    }
    val out = process.inputReader().readText().trim()
    val argMax = out.toIntOrNull()
    if (argMax == null) {
      bspClientLogger.warn("Failed to parse ARG_MAX. Stdout: $out")
    }
    return argMax
  }
}
