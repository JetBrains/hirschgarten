package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.remoteDev.util.addPathSuffix
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.net.URI
import javax.swing.JComponent
import kotlin.io.path.toPath

class BazelBspJumpToBuildFileAction(
  component: JComponent,
  private val project: Project,
  private val buildTargetInfo: BuildTargetInfo,
) : AnAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  init {
    registerCustomShortcutSet(CommonShortcuts.getEditSource(), component)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val buildFile = findBuildFile(project, buildTargetInfo) ?: return
    EditorHelper.openInEditor(buildFile, true, true)
  }
}

fun findBuildFile(project: Project, buildTargetInfo: BuildTargetInfo): PsiFile? {
  val baseDirectoryPath = buildTargetInfo.baseDirectory?.let { URI.create(it) } ?: return null
  val buildFilePath = baseDirectoryPath.addPathSuffix("BUILD").toPath()
  val buildBazelFilePath = baseDirectoryPath.addPathSuffix("BUILD.bazel").toPath()
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(buildFilePath) ?: virtualFileManager.findFileByNioPath(buildBazelFilePath) ?: return null
  return PsiManager.getInstance(project).findFile(virtualFile)
}
