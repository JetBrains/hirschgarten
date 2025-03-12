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
package org.jetbrains.bazel.ogRun

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.dependencies.TargetInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.ogRun.other.TestSize
import java.io.File
import java.util.*

/**
 * Matches source files to test targets, if the source file is present in the test target's 'srcs'
 * list. Only looks for exact matches.
 */
class TestTargetSourcesHeuristic : TestTargetHeuristic {
  override fun matchesSource(
    project: Project?,
    target: TargetInfo,
    sourcePsiFile: PsiFile?,
    sourceFile: File?,
    testSize: TestSize?,
  ): Boolean {
    val sources: Optional<ImmutableList<ArtifactLocation?>?> = target.getSources()
    if (!sources.isPresent()) {
      return false
    }
    val projectData: BlazeProjectData? =
      BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
    if (projectData == null) {
      return false
    }

    val decoder: ArtifactLocationDecoder = projectData.getArtifactLocationDecoder()
    for (src in sources.get()) {
      if (decoder.resolveSource(src) == sourceFile) {
        return true
      }
    }
    return false
  }
}
