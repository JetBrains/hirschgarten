package org.jetbrains.bazel.clion.run.debug

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.buildFlagsEndOffset
import org.jetbrains.bazel.languages.projectview.psi.setCompilationMode

/** Returns null if the project has no project view file. */
internal suspend fun findProjectViewFile(project: Project): VirtualFile? {
  val path = ProjectViewService.getInstance(project).projectViewPath ?: return null
  return withContext(Dispatchers.IO) { VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path) }
}

internal suspend fun openProjectViewAtBuildFlags(project: Project, file: VirtualFile) {
  val offset = readAction { findProjectViewPsi(project, file)?.buildFlagsEndOffset() } ?: 0

  withContext(Dispatchers.EDT) {
    OpenFileDescriptor(project, file, offset).navigate(true)
  }
}

/**
 * Writes `--compilation_mode=dbg` to the `build_flags` of the project view file.
 * The server reads the new flags only after the reparse of the project view.
 */
@Throws(ExecutionException::class)
context(ctx: CcDebugContext)
internal suspend fun setDbgCompilationMode() {
  val file = findProjectViewFile(ctx.project)
    ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.project.view.not.found"))

  writeCommandAction(ctx.project, BazelCLionCoreBundle.message("debug.info.command.set.compilation.mode")) {
    val psi = findProjectViewPsi(ctx.project, file)
      ?: throw ExecutionException(BazelCLionCoreBundle.message("debug.error.project.view.not.found"))

    psi.setCompilationMode("dbg")
  }

  saveAllFiles()
  ProjectViewService.getInstance(ctx.project).forceReparseCurrentProjectViewFiles()
}

private fun findProjectViewPsi(project: Project, file: VirtualFile): ProjectViewPsiFile? {
  return PsiManager.getInstance(project).findFile(file) as? ProjectViewPsiFile
}
