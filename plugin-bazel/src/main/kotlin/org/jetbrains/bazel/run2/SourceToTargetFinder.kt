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
package org.jetbrains.bazel.run2

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import java.io.File
import java.util.concurrent.Future
import java.util.function.Predicate

/**
 * Searches through the transitive rdeps map for blaze rules of a certain type which build a given
 * source file.
 */
interface SourceToTargetFinder {
  /**
   * Finds all rules of the given type 'reachable' from source file (i.e. with source included in
   * srcs, deps or runtime_deps).
   */
  fun targetsForSourceFile(
    project: Project,
    sourceFile: File,
    ruleType: RuleType?,
  ): Future<Collection<TargetInfo>> = targetsForSourceFiles(project, ImmutableSet.of(sourceFile), ruleType)

  /**
   * Finds all rules of the given type 'reachable' from the given source files (i.e. with one of the
   * sources included in srcs, deps or runtime_deps).
   */
  fun targetsForSourceFiles(
    project: Project,
    sourceFiles: Set<File>,
    ruleType: RuleType?,
  ): Future<Collection<TargetInfo>>

  companion object {
    /**
     * Iterates through the all [SourceToTargetFinder]'s, returning a [Future]
     * representing the first non-empty result, prioritizing any which are immediately available.
     *
     *
     * Future returns null if there was no non-empty result found.
     */
    fun findTargetInfoFuture(
      project: Project,
      sourceFile: File,
      ruleType: RuleType?,
    ): ListenableFuture<Collection<TargetInfo>> = findTargetInfoFuture(project, ImmutableSet.of(sourceFile), ruleType)

    /**
     * Iterates through the all [SourceToTargetFinder]'s, returning a [Future]
     * representing the first non-empty result, prioritizing any which are immediately available.
     *
     *
     * Future returns null if there was no non-empty result found.
     */
    fun findTargetInfoFuture(
      project: Project,
      sourceFiles: Set<File>,
      ruleType: RuleType?,
    ): ListenableFuture<Collection<TargetInfo>> {
      val futures: Iterable<Future<Collection<TargetInfo>>> =
        Iterables.transform(
          EP_NAME.extensionList,
        ) { it.targetsForSourceFiles(project, sourceFiles, ruleType) }
      return FuturesUtil.getFirstFutureSatisfyingPredicate<Collection<TargetInfo?>?>(
        futures,
        Predicate { t: Collection<TargetInfo> -> !t.isEmpty() },
      )
    }

    /**
     * Iterates through all [SourceToTargetFinder]s, returning the first immediately available,
     * non-empty result.
     */
    fun findTargetsForSourceFile(
      project: Project,
      sourceFile: File,
      ruleType: RuleType?,
    ): Collection<TargetInfo> {
      val future =
        findTargetInfoFuture(project, sourceFile, ruleType)
      if (future.isDone) {
        val targets: Collection<TargetInfo> =
          FuturesUtil.getIgnoringErrors<Collection<TargetInfo?>?>(future)
        if (!targets.isEmpty()) {
          return targets
        }
      }
      return ImmutableList.of()
    }

    val EP_NAME: ExtensionPointName<SourceToTargetFinder> =
      ExtensionPointName.create("com.google.idea.blaze.SourceToTargetFinder")
  }
}
