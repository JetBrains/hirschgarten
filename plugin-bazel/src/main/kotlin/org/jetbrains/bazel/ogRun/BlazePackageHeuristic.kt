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

import com.google.idea.blaze.base.bazel.BuildSystemProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.ogRun.other.TestSize
import java.io.File

/** Looks for a test rule in the same blaze package as the source file.  */
internal class BlazePackageHeuristic : TestTargetHeuristic {
  override fun matchesSource(
    project: Project?,
    target: TargetInfo,
    sourcePsiFile: PsiFile?,
    sourceFile: File?,
    testSize: TestSize?,
  ): Boolean {
    val vf =
      if (sourcePsiFile != null) {
        sourcePsiFile.getVirtualFile()
      } else {
        VfsUtils.resolveVirtualFile(sourceFile, /* refreshIfNeeded= */true)
      }
    val sourcePackage: WorkspacePath? = findBlazePackage(project, vf)
    if (sourcePackage == null) {
      return false
    }
    val targetPackage: WorkspacePath? = target.label.blazePackage()
    return sourcePackage.equals(targetPackage)
  }

  companion object {
    private fun findBlazePackage(project: Project?, vf: VirtualFile?): WorkspacePath? {
      var vf = vf
      val provider: BuildSystemProvider = Blaze.getBuildSystemProvider(project)
      val root: WorkspaceRoot? = WorkspaceRoot.fromProjectSafe(project)
      if (root == null) {
        return null
      }
      while (vf != null) {
        if (vf.isDirectory() && provider.findBuildFileInDirectory(vf) != null) {
          return root.workspacePathForSafe(File(vf.getPath()))
        }
        vf = vf.parent
      }
      return null
    }
  }
}
