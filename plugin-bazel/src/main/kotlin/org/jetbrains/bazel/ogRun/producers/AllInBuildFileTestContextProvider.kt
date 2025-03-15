/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.producers

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import java.io.File

/** Runs all tests in a single selected BUILD file.  */
internal class AllInBuildFileTestContextProvider : TestContextProvider {
  override fun getTestContext(context: ConfigurationContext): RunConfigurationContext? {
    val location = context.psiLocation

    if (location !is PsiFile) {
      return null
    }

    val parent = location.parent

    if (!isBuildFile(context, location) || parent == null) {
      return null
    }

    val root: WorkspaceRoot = WorkspaceRoot.fromProject(context.project)
    return fromDirectoryNonRecursive(root, parent)
  }

  companion object {
    private fun fromDirectoryNonRecursive(root: WorkspaceRoot, dir: PsiDirectory): RunConfigurationContext? {
      val packagePath: WorkspacePath? = getWorkspaceRelativePath(root, dir.getVirtualFile()) ?: return null
      return RunConfigurationContext.fromKnownTarget(
        Label.allFromPackageNonRecursive(packagePath),
        BlazeCommandName.TEST,
        dir,
      )
    }

    private fun getWorkspaceRelativePath(root: WorkspaceRoot, vf: VirtualFile): WorkspacePath? =
      root.workspacePathForSafe(File(vf.path))

    private fun isBuildFile(context: ConfigurationContext, file: PsiFile): Boolean =
      Blaze.getBuildSystemProvider(context.project).isBuildFile(file.name)
  }
}
