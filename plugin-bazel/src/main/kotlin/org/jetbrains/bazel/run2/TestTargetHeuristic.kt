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

import com.google.common.base.Function
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.run2.targetfinder.FuturesUtil
import org.jetbrains.ide.PooledThreadExecutor
import java.io.File
import java.util.Optional
import java.util.stream.Collectors

/** Heuristic to match test targets to source files.  */
interface TestTargetHeuristic {
  /** Returns true if the rule and source file match, according to this heuristic.  */
  fun matchesSource(
    project: Project,
    target: BspTargetInfo.TargetInfo,
    sourcePsiFile: PsiFile,
    sourceFile: File,
    testSize: TestSize
  ): Boolean

  companion object {
    /**
     * Finds a test rule associated with a given [PsiElement]. Must be called from within a read
     * action.
     */
    fun targetFutureForPsiElement(
      element: PsiElement, testSize: TestSize
    ): ListenableFuture<BspTargetInfo.TargetInfo>? {
      val psiFile = element.containingFile
      if (psiFile == null) {
        return null
      }
      val vf = psiFile.virtualFile
      val file = if (vf != null) File(vf.path) else null
      if (file == null) {
        return null
      }
      val project = element.project
      val targets =
        SourceToTargetFinder.findTargetInfoFuture(project, file, RuleType.TEST)
      if (targets.isDone && FuturesUtil.getIgnoringErrors<MutableCollection<BspTargetInfo.TargetInfo?>?>(targets) == null) {
        return null
      }
      val executor =
        if (ApplicationManager.getApplication().isUnitTestMode)
          MoreExecutors.directExecutor()
        else
          PooledThreadExecutor.INSTANCE
      return Futures.transform(
        targets,
        { list ->
          if (list == null)
            null
          else
            chooseTestTargetForSourceFile(
              project, psiFile, file, list, testSize
            )
        },
        executor
      )
    }

    /**
     * Given a source file and all test rules reachable from that file, chooses a test rule based on
     * available filters, falling back to choosing the first one if there is no match.
     */
    fun chooseTestTargetForSourceFile(
      project: Project,
      sourcePsiFile: PsiFile,
      sourceFile: File,
      targets: Collection<BspTargetInfo.TargetInfo>,
      testSize: TestSize
    ): BspTargetInfo.TargetInfo? {
      if (targets.isEmpty()) {
        return null
      }
      var filteredTargets: MutableList<BspTargetInfo.TargetInfo> = ArrayList(targets)
      for (filter in EP_NAME.extensions) {
        val matches =
          filteredTargets
            .stream()
            .filter { target ->
              filter.matchesSource(
                project,
                target,
                sourcePsiFile,
                sourceFile,
                testSize
              )
            }
            .collect(Collectors.toList())
        if (matches.size == 1) {
          return matches.get(0)
        }
        if (!matches.isEmpty()) {
          // A higher-priority filter found more than one match -- subsequent filters will only
          // consider these matches.
          filteredTargets = matches
        }
      }
      // finally order by syncTime (if available), returning the most recently synced
      return filteredTargets.stream()
        .max(
          Comparator.comparing(
             { t -> t.syncTime }, Comparator.nullsFirst(
              Comparator.naturalOrder()
            )
          )
        )
        .orElse(null)
    }

    val EP_NAME: ExtensionPointName<TestTargetHeuristic> =
      ExtensionPointName.create<TestTargetHeuristic>("com.google.idea.blaze.TestTargetHeuristic")
  }
}
