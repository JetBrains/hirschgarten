package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.remoteDev.util.addPathSuffix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getEditor
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import java.net.URI
import kotlin.io.path.toPath

class BazelBspJumpToBuildFileAction(private val buildTargetInfo: BuildTargetInfo?) :
  SuspendableAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  // Used in plugin.xml for source file popup menu
  @Suppress("UNUSED")
  constructor() : this(null)

  override fun update(project: Project, e: AnActionEvent) {
    // buildTargetInfo is provided for the target widget, but not if the action is invoked via the popup menu on a source file
    if (buildTargetInfo != null) return

    e.presentation.isEnabledAndVisible = (e.place == ActionPlaces.EDITOR_POPUP || e.place == ActionPlaces.KEYBOARD_SHORTCUT) &&
      e.getPsiFile()?.virtualFile?.let {
        project.targetUtils.getTargetsForFile(it).isNotEmpty()
      } == true
  }

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val buildTargetInfo =
      this.buildTargetInfo ?: run {
        val virtualFile = readAction { e.getPsiFile()?.virtualFile } ?: return
        getBuildTargetInfo(project, virtualFile, e.getEditor()) ?: return
      }
    jumpToBuildFile(project, buildTargetInfo)
  }

  private suspend fun getBuildTargetInfo(
    project: Project,
    file: VirtualFile,
    editor: Editor?,
  ): BuildTargetInfo? {
    val target = project.targetUtils.getTargetsForFile(file).chooseTarget(editor) ?: return null
    return project.targetUtils.getBuildTargetInfoForLabel(target)
  }
}

suspend fun jumpToBuildFile(project: Project, buildTargetInfo: BuildTargetInfo) {
  val buildFile =
    readAction {
      findBuildFileTarget(project, buildTargetInfo)
    } ?: return
  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(buildFile, true, true)
  }
}

private fun findBuildFileTarget(project: Project, buildTargetInfo: BuildTargetInfo): PsiElement? {
  val buildFile = findBuildFile(project, buildTargetInfo) ?: return null

  // Try to jump to a specific target
  val target = buildTargetInfo.id.target as? SingleTarget
  if (target != null) {
    (buildFile as? StarlarkFile)?.findRuleTarget(target.targetName)?.let { return it }
  }
  // Fallback to the BUILD file itself
  return buildFile
}

fun findBuildFile(project: Project, buildTargetInfo: BuildTargetInfo): StarlarkFile? {
  val baseDirectoryPath = buildTargetInfo.baseDirectory?.let { URI.create(it) } ?: return null
  // Sometimes a project can contain a directory named "build" (which on case-insensitive filesystems is the same as BUILD).
  // Try with BUILD.bazel first to avoid this case.
  val buildBazelFilePath = baseDirectoryPath.addPathSuffix("BUILD.bazel").toPath()
  val buildFilePath = baseDirectoryPath.addPathSuffix("BUILD").toPath()
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(buildBazelFilePath)?.takeIf { it.isFile }
      ?: virtualFileManager.findFileByNioPath(buildFilePath)?.takeIf { it.isFile }
      ?: return null
  return PsiManager.getInstance(project).findFile(virtualFile) as? StarlarkFile
}
