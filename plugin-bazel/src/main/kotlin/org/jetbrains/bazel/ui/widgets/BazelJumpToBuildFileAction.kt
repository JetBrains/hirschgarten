package org.jetbrains.bazel.ui.widgets

import com.intellij.icons.AllIcons
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock
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
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.JComponent

sealed class BazelJumpToBuildFileAction :
  SuspendableAction({ BazelPluginBundle.message("widget.open.build.file") }, AllIcons.Actions.OpenNewTab) {
  protected abstract suspend fun getTargetLabel(project: Project, e: AnActionEvent): Label?

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val target = getTargetLabel(project, e) ?: return
    jumpToBuildFile(project, target)
  }

  @Suppress("ComponentNotRegistered", "unused") // it is registered in plugin.xml
  class XmlRegistered : BazelJumpToBuildFileAction() {
    override suspend fun getTargetLabel(project: Project, e: AnActionEvent): Label? {
      val virtualFile = readAction { e.getPsiFile()?.virtualFile } ?: return null
      return project.targetUtils.getTargetsForFile(virtualFile).chooseTarget(e.getEditor())
    }

    override fun update(project: Project, e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = shouldBeEnabledAndVisible(project, e)
    }

    private fun shouldBeEnabledAndVisible(project: Project, e: AnActionEvent): Boolean =
      e.getPsiFile()?.virtualFile?.let {
        project.targetUtils.getTargetsForFile(it).isNotEmpty()
      } == true
  }

  class NonXmlRegistered(private val getTarget: () -> BuildTarget?) : BazelJumpToBuildFileAction() {
    override suspend fun getTargetLabel(project: Project, e: AnActionEvent): Label? = getTarget()?.id

    fun registerShortcut(component: JComponent) {
      val shortcutSet = KeymapUtil.getActiveKeymapShortcuts("EditSource")
      registerCustomShortcutSet(shortcutSet, component)
    }
  }
}

suspend fun jumpToBuildFile(project: Project, target: Label) {
  val definition =
    readAction {
      findDefinition(project, target)
    } ?: return

  withContext(Dispatchers.EDT) {
    EditorHelper.openInEditor(definition, true, true)
  }
}

@RequiresReadLock
private fun findDefinition(project: Project, target: Label): PsiElement? {
  val file = resolveLabel(project, target)
  if (file !is StarlarkFile) return file

  val definition = findDefinitionByLongestPrefix(file, target)
  return definition ?: file
}

/**
 * Uses a simple longest prefix heuristic to find the corresponding definition
 * inside the BUILD file. Similar to what the Google plugin does.
 */
private fun findDefinitionByLongestPrefix(file: StarlarkFile, target: Label): PsiElement? {
  val name = target.targetName

  var candidate: PsiElement? = null
  var candidateScore = 0

  for (expr in file.findChildrenByClass(StarlarkExpressionStatement::class.java)) {
    val score = getCallCommonPrefix(expr, name)

    if (score > candidateScore) {
      candidate = expr
      candidateScore = score
    }
  }

  return candidate
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
