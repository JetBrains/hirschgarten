package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getEditor
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.references.resolveLabel
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
      resolveLabel(project, target)
    } ?: return
  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(buildFile, true, true)
  }
}

// if the action is triggered from idea vim
private const val IDEA_VIM_ACTION_PLACE = "IdeaVim"

private val ALLOWED_ACTION_PLACES =
  listOf(
    ActionPlaces.EDITOR_POPUP,
    ActionPlaces.KEYBOARD_SHORTCUT,
    ActionPlaces.EDITOR_TAB_POPUP,
    ActionPlaces.ACTION_SEARCH,
    IDEA_VIM_ACTION_PLACE,
  )
