/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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

import com.google.common.collect.ImmutableMap
import com.google.idea.blaze.base.dependencies.TargetInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.ogRun.other.TestSize
import java.io.File
import java.util.Map

/**
 * For cases where the target's size attribute isn't known, try to guess the size from portions of
 * the target name. This is a very rough heuristic, and should be run near the end, after more
 * precise heuristics.
 */
class TestSizeFromRoughTargetNameHeuristic : TestTargetHeuristic {
  override fun matchesSource(
    project: Project?,
    target: TargetInfo,
    sourcePsiFile: PsiFile?,
    sourceFile: File?,
    testSize: TestSize?,
  ): Boolean {
    var testSize: TestSize? = testSize
    if (target.testSize != null) {
      return false // no need to guess, we already know the target's size attribute
    }
    // if no size annotation is present, treat as small tests (b/33503928).
    if (testSize == null) {
      testSize = TestSize.SMALL
    }
    return testSize === guessTargetTestSize(target)
  }

  companion object {
    private val TARGET_NAME_TO_TEST_SIZE: ImmutableMap<String?, TestSize?> =
      ImmutableMap.of<String?, TestSize?>(
        "small",
        TestSize.SMALL,
        "medium",
        TestSize.MEDIUM,
        "large",
        TestSize.LARGE,
        "enormous",
        TestSize.ENORMOUS,
      )

    /** Looks for an substring match between the rule name and the test size annotation class name.  */
    private fun guessTargetTestSize(target: TargetInfo): TestSize? {
      val ruleName =
        target.label
          .targetName()
          .toString()
          .toLowerCase()
      return TARGET_NAME_TO_TEST_SIZE.entries
        .stream()
        .filter { entry: MutableMap.MutableEntry<String?, TestSize?>? -> ruleName.contains(entry!!.key!!) }
        .map<TestSize?> { Map.Entry.value }
        .findFirst()
        .orElse(null)
    }
  }
}
