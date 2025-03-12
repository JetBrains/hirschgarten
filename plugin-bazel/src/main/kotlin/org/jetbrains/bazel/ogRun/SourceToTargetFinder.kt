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
package org.jetbrains.bazel.ogRun

import com.google.common.base.Function
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.ListenableFuture
import com.google.idea.blaze.base.dependencies.TargetInfo
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import java.io.File
import java.util.*
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
        project: Project?, sourceFile: File, ruleType: Optional<RuleType?>?
    ): Future<MutableCollection<TargetInfo?>?>? {
        return targetsForSourceFiles(project, ImmutableSet.of<File?>(sourceFile), ruleType)
    }

    /**
     * Finds all rules of the given type 'reachable' from the given source files (i.e. with one of the
     * sources included in srcs, deps or runtime_deps).
     */
    fun targetsForSourceFiles(
        project: Project?, sourceFiles: MutableSet<File?>?, ruleType: Optional<RuleType?>?
    ): Future<MutableCollection<TargetInfo?>?>?

    companion object {
        /**
         * Iterates through the all [SourceToTargetFinder]'s, returning a [Future]
         * representing the first non-empty result, prioritizing any which are immediately available.
         *
         *
         * Future returns null if there was no non-empty result found.
         */
        fun findTargetInfoFuture(
            project: Project?, sourceFile: File, ruleType: Optional<RuleType?>?
        ): ListenableFuture<MutableCollection<TargetInfo?>?> {
            return findTargetInfoFuture(project, ImmutableSet.of<File?>(sourceFile), ruleType)
        }

        /**
         * Iterates through the all [SourceToTargetFinder]'s, returning a [Future]
         * representing the first non-empty result, prioritizing any which are immediately available.
         *
         *
         * Future returns null if there was no non-empty result found.
         */
        fun findTargetInfoFuture(
            project: Project?, sourceFiles: MutableSet<File?>?, ruleType: Optional<RuleType?>?
        ): ListenableFuture<MutableCollection<TargetInfo?>?> {
            val futures: Iterable<Future<MutableCollection<TargetInfo?>?>?> =
                Iterables.transform<SourceToTargetFinder?, Future<MutableCollection<TargetInfo?>?>?>(
                    Arrays.asList<SourceToTargetFinder?>(*EP_NAME.extensions),
                    Function { f: SourceToTargetFinder? -> f!!.targetsForSourceFiles(project, sourceFiles, ruleType) })
            return FuturesUtil.getFirstFutureSatisfyingPredicate<MutableCollection<TargetInfo?>?>(
                futures,
                Predicate { t: MutableCollection<TargetInfo?>? -> t != null && !t.isEmpty() })
        }

        /**
         * Iterates through all [SourceToTargetFinder]s, returning the first immediately available,
         * non-empty result.
         */
        fun findTargetsForSourceFile(
            project: Project?, sourceFile: File, ruleType: Optional<RuleType?>?
        ): MutableCollection<TargetInfo?> {
            val future: ListenableFuture<MutableCollection<TargetInfo?>?> =
                findTargetInfoFuture(project, sourceFile, ruleType)
            if (future.isDone()) {
                val targets: MutableCollection<TargetInfo?>? =
                    FuturesUtil.getIgnoringErrors<MutableCollection<TargetInfo?>?>(future)
                if (targets != null && !targets.isEmpty()) {
                    return targets
                }
            }
            return ImmutableList.of<TargetInfo?>()
        }

        val EP_NAME: ExtensionPointName<SourceToTargetFinder?> =
            create.create<SourceToTargetFinder?>("com.google.idea.blaze.SourceToTargetFinder")
    }
}
