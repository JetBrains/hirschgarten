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
package org.jetbrains.bazel.run2

import com.google.common.collect.ImmutableSet
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor

/** Run configuration executor type  */
enum class ExecutorType {
  RUN,
  FAST_BUILD_RUN,
  DEBUG,
  FAST_BUILD_DEBUG,
  COVERAGE,
  DEBUG_STARLARK,
  UNKNOWN,
  ;

  val isDebugType: Boolean
    get() = this == DEBUG || this == FAST_BUILD_DEBUG

  val isFastBuildType: Boolean
    get() = this == FAST_BUILD_RUN || this == FAST_BUILD_DEBUG

  companion object {
    @JvmStatic
    fun fromExecutor(executor: Executor): ExecutorType = fromExecutorId(executor.id)

    @JvmStatic
    fun fromExecutorId(executorId: String): ExecutorType {
      if (executorId == DefaultRunExecutor.EXECUTOR_ID) {
        return RUN
      }
      // hard-code string because this class doesn't exist in the CLion plugin
      if (executorId == "BlazeFastRun") {
        return FAST_BUILD_RUN
      }
      if (executorId == DefaultDebugExecutor.EXECUTOR_ID) {
        return DEBUG
      }
      // hard-code string because this class doesn't exist in the CLion plugin
      if (executorId == "BlazeFastDebug") {
        return FAST_BUILD_DEBUG
      }
      // hard-code string to avoid plugin dependency (coverage plugin not yet available in CLion)
      if (executorId == "Coverage") {
        return COVERAGE
      }

      if (executorId == "SkylarkDebugExecutor") {
        return DEBUG_STARLARK
      }
      return UNKNOWN
    }

    /** Executor types supported for debuggable targets.  */
    val DEBUG_SUPPORTED_TYPES: ImmutableSet<ExecutorType?> =
      ImmutableSet.of<ExecutorType?>(RUN, DEBUG, COVERAGE)

    /** Executor types supported for non-debuggable targets.  */
    val DEBUG_UNSUPPORTED_TYPES: ImmutableSet<ExecutorType?> =
      ImmutableSet.of<ExecutorType?>(RUN, COVERAGE)

    /** Executor types supported for targets supporting fast run/debug.  */
    val FAST_DEBUG_SUPPORTED_TYPES: ImmutableSet<ExecutorType?> =
      ImmutableSet.of<ExecutorType?>(
        RUN,
        FAST_BUILD_RUN,
        DEBUG,
        FAST_BUILD_DEBUG,
        COVERAGE,
      )
  }
}
