package org.jetbrains.bazel.flow.open

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.coroutines.CoroutineService
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.flow.open.BspStartupActivity
import org.jetbrains.plugins.bsp.flow.open.initProperties
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction

private val ELIGIBLE_BAZEL_PROJECT_FILE_NAMES = listOf(
  "projectview.bazelproject",
  "WORKSPACE",
  "WORKSPACE.bazel",
  "MODULE.bazel",
  "WORKSPACE.bzlmod",
)

internal class OpenBazelProjectViaBspPluginAction:
  SuspendableAction({ "Open Bazel project"}), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)

    val projectRootDir = calculateProjectRootDir(project, psiFile)
    performOpenBazelProjectViaBspPlugin(project, projectRootDir)
  }

  override fun update(project: Project, e: AnActionEvent) {
    val psiFile = CommonDataKeys.PSI_FILE.getData(e.dataContext)

    val importable = isImportableBazelBspProject(project, psiFile)

    e.presentation.isEnabled = importable
    e.presentation.isVisible =
      psiFile?.virtualFile?.name?.let { ELIGIBLE_BAZEL_PROJECT_FILE_NAMES.contains(it) } == true && importable
  }

  private fun isImportableBazelBspProject(project: Project?, psiFile: PsiFile? = null): Boolean {
    if (project?.isBspProject == true) return false

    val projectRootDir = calculateProjectRootDir(project, psiFile) ?: return false

    return BazelBspProjectOpenProcessor().canOpenProject(projectRootDir)
  }

  private fun calculateProjectRootDir(project: Project?, psiFile: PsiFile?): VirtualFile? =
    psiFile?.virtualFile?.parent ?: project?.guessProjectDir()
}

fun performOpenBazelProjectViaBspPlugin(project: Project?, projectRootDir: VirtualFile?) {
  if (projectRootDir != null && project != null) {
    project.initProperties(projectRootDir, bazelBspBuildToolId)
    CoroutineService.getInstance(project).start {
      BspStartupActivity().execute(project)
    }
  }
}
