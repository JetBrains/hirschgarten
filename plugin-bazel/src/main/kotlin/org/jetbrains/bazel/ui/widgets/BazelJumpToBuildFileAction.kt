package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
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
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.target.targetUtils

class BazelJumpToBuildFileAction(private val target: Label?) :
  SuspendableAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  // Used in plugin.xml for source file popup menu
  @Suppress("UNUSED")
  constructor() : this(null)

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = shouldBeEnabledAndVisible(project, e)
  }

  private fun shouldBeEnabledAndVisible(project: Project, e: AnActionEvent): Boolean {
    if (target != null) {
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
    val target =
      this.target ?: run {
        val virtualFile = readAction { e.getPsiFile()?.virtualFile } ?: return
        project.targetUtils.getTargetsForFile(virtualFile).chooseTarget(e.getEditor()) ?: return
      }
    jumpToBuildFile(project, target)
  }
}

suspend fun jumpToBuildFile(project: Project, target: Label) {
  val buildFile =
    readAction {
      findBuildFileTarget(project, target)
    } ?: return
  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(buildFile, true, true)
  }
}

fun findBuildFileTarget(project: Project, label: Label): PsiElement? {
  val buildFile = findBuildFile(project, label) ?: return null
  // Try to jump to a specific target
  val target = label.target as? SingleTarget
  if (target != null) {
    val ruleTarget = buildFile.findRuleTarget(target.targetName)
    return ruleTarget?.getArgumentList()?.getNameArgument()
  }
  // Fallback to the BUILD file itself
  return buildFile
}

fun findBuildFile(project: Project, target: Label): StarlarkFile? {
  val buildTarget = project.targetUtils.getBuildTargetForLabel(target) ?: return null
  // Sometimes a project can contain a directory named "build" (which on case-insensitive filesystems is the same as BUILD).
  // Try with BUILD.bazel first to avoid this case.
  val buildBazelFilePath = buildTarget.baseDirectory.resolve("BUILD.bazel")
  val buildFilePath = buildTarget.baseDirectory.resolve("BUILD")
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
