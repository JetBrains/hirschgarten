/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.run2.targetfinder

import com.google.common.base.Function
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import java.util.Arrays
import java.util.Objects
import java.util.concurrent.Future
import java.util.function.Predicate

/** Finds information about targets matching a given label.  */
interface TargetFinder {
  /** Returns a future for a [TargetInfo] corresponding to the given blaze label.  */
  fun findTarget(project: Project, label: Label): Future<TargetInfo?>

  companion object {
    /**
     * Iterates through all [TargetFinder]s, returning a [Future] representing the first
     * non-null result, prioritizing any which are immediately available.
     *
     *
     * Future returns null if this no non-null result was found.
     */
    fun findTargetInfoFuture(project: Project, label: Label): ListenableFuture<TargetInfo?> {
      val futures = EP_NAME.extensionList.map { it.findTarget(project, label) }
      return FuturesUtil.getFirstFutureSatisfyingPredicate(
        futures
      ) { obj: TargetInfo? -> Objects.nonNull(obj) }
    }

    /**
     * Iterates through all [TargetFinder]s, returning the first immediately available, non-null
     * result.
     */
    fun findTargetInfo(project: Project, label: Label): TargetInfo? {
      val future = findTargetInfoFuture(project, label)
      return if (future.isDone) FuturesUtil.getIgnoringErrors<TargetInfo?>(future) else null
    }

    val EP_NAME: ExtensionPointName<TargetFinder> = ExtensionPointName("com.google.idea.blaze.TargetFinder")
  }
}
