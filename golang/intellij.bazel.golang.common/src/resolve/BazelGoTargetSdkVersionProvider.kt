package org.jetbrains.bazel.golang.resolve

import com.goide.sdk.GoSdkService
import com.goide.sdk.GoTargetSdkVersionProvider
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil

/**
 * rules_go with Bazel has no distinction between compile and target SDK versions,
 * unlike, e.g., go.mod` where you can set the Go version explicitly.
 */
internal class BazelGoTargetSdkVersionProvider : GoTargetSdkVersionProvider {
  override fun isApplicable(file: PsiFile): Boolean =
    file.project.isBazelProject

  override fun getTargetSdkVersion(file: PsiFile): String? {
    val project = file.project
    val module = GoWorkspaceModuleUtil.findModule(project) ?: return null
    return GoSdkService.getInstance(project).getSdk(module).majorVersion.toString()
  }
}
