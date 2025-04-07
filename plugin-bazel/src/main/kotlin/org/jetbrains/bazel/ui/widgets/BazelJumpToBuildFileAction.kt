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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getEditor
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTarget

class BazelJumpToBuildFileAction(private val buildTarget: BuildTarget?) :
  SuspendableAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  // Used in plugin.xml for source file popup menu
  @Suppress("UNUSED")
  constructor() : this(null)

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = shouldBeEnabledAndVisible(project, e)
  }

  private fun shouldBeEnabledAndVisible(project: Project, e: AnActionEvent): Boolean {
    if (buildTarget != null) {
      // Action was created via BazelFileTargetsWidget. In this case `e.getPsiFile()` is `null`, but we should enable the action regardless.
      return true
    }
    return (
      ALLOWED_ACTION_PLACES.contains(e.place) &&
        e.getPsiFile()?.virtualFile?.let {
          project.targetUtils.getTargetsForFile(it).isNotEmpty()
        } == true
    )
  }

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val buildTarget =
      this.buildTarget ?: run {
        val virtualFile = readAction { e.getPsiFile()?.virtualFile } ?: return
        getBuildTarget(project, virtualFile, e.getEditor()) ?: return
      }
    jumpToBuildFile(project, buildTarget)
  }

  private suspend fun getBuildTarget(
    project: Project,
    file: VirtualFile,
    editor: Editor?,
  ): BuildTarget? {
    val target = project.targetUtils.getTargetsForFile(file).chooseTarget(editor) ?: return null
    return project.targetUtils.getBuildTargetForLabel(target)
  }
}

suspend fun jumpToBuildFile(project: Project, buildTarget: BuildTarget) {
  val buildFile =
    readAction {
      findBuildFileTarget(project, buildTarget)
    } ?: return
  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(buildFile, true, true)
  }
}

private fun findBuildFileTarget(project: Project, buildTarget: BuildTarget): PsiElement? {
  val buildFile = findBuildFile(project, buildTarget) ?: return null

  // Try to jump to a specific target
  val target = buildTarget.id.target as? SingleTarget
  if (target != null) {
    buildFile.findRuleTarget(target.targetName)?.let { return it }
  }
  // Fallback to the BUILD file itself
  return buildFile
}

fun findBuildFile(project: Project, buildTarget: BuildTarget): StarlarkFile? {
  val baseDirectoryPath = buildTarget.baseDirectory ?: return null
  // Sometimes a project can contain a directory named "build" (which on case-insensitive filesystems is the same as BUILD).
  // Try with BUILD.bazel first to avoid this case.
  val buildBazelFilePath = baseDirectoryPath.resolve("BUILD.bazel")
  val buildFilePath = baseDirectoryPath.resolve("BUILD")
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(buildBazelFilePath)?.takeIf { it.isFile }
      ?: virtualFileManager.findFileByNioPath(buildFilePath)?.takeIf { it.isFile }
      ?: return null
  return PsiManager.getInstance(project).findFile(virtualFile) as? StarlarkFile
}

private val ALLOWED_ACTION_PLACES =
  listOf(
    ActionPlaces.EDITOR_POPUP,
    ActionPlaces.KEYBOARD_SHORTCUT,
    ActionPlaces.EDITOR_TAB_POPUP,
  )
