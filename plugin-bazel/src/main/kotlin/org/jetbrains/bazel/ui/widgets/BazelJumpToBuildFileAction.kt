package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getEditor
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
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
    return e.getPsiFile()?.virtualFile?.let {
      project.targetUtils.getTargetsForFile(it).isNotEmpty()
    } == true
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
  val buildFile = readAction {
    resolveLabel(project, target)
  } ?: return

  val definition = if (buildFile is StarlarkFile && Registry.`is`("bazel.use.longest.prefix.heuristic")) {
    findDefinitionByLongestPrefix(buildFile, target) ?: buildFile
  }
  else {
    buildFile
  }

  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(definition, true, true)
  }
}

/**
 * Uses a simple longest prefix heuristic to find the corresponding definition
 * inside the BUILD file. Similar to what the Google plugin does.
 */
private fun findDefinitionByLongestPrefix(file: StarlarkFile, target: Label): PsiElement? {
  val name = target.targetName

  return file.findChildrenByClass(StarlarkExpressionStatement::class.java).maxByOrNull {
    getCallCommonPrefix(it, name)
  }
}

private fun getCallCommonPrefix(expr: StarlarkExpressionStatement, value: String): Int {
  val name = getCallName(expr) ?: return 0
  return name.commonPrefixWith(value).length
}

private fun getCallName(expr: StarlarkExpressionStatement): String? {
  val call = expr.callExpressionOrNull() ?: return null
  val args = call.getArgumentList() ?: return null
  val name = args.getKeywordArgument("name") ?: return null
  return name.getArgumentStringValue()
}