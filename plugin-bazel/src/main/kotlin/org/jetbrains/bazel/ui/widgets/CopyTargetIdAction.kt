package org.jetbrains.bazel.ui.widgets

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ui.TextTransferable
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.action.getEditor
import org.jetbrains.bazel.action.getPsiFile
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1158

/**
 * Action is available in a BUILD file in the right click Editor Popup Menu under Copy/Paste Special
 * while pressing on rule's name or value of the name argument of that rule.
 * Also available in the right click menu on regular source files that are included in project.
 */
internal class CopyTargetIdAction : SuspendableAction({ BazelPluginBundle.message("editor.action.copy.target.id") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    performCopyTargetIdAction(
      psiFile = readAction { e.getPsiFile() } ?: return,
      psiElement = readAction { e.getPsiElement() },
      editor = e.getEditor(),
    )
  }

  private suspend fun performCopyTargetIdAction(
    psiFile: PsiFile,
    psiElement: PsiElement?,
    editor: Editor?,
  ) {
    val targetId = calculateTargetId(psiFile, psiElement, editor) ?: return
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(targetId as CharSequence)
    clipboard.setContents(transferable)
  }

  private suspend fun calculateTargetId(
    psiFile: PsiFile,
    psiElement: PsiElement?,
    editor: Editor?,
  ): String? {
    if (psiFile is StarlarkFile) {
      return readAction { psiElement?.findParentOfType<StarlarkCallExpression>()?.calculateTargetId() }
    } else {
      val virtualFile = readAction { psiFile.virtualFile } ?: return null
      return psiFile.project.targetUtils
        .getTargetsForFile(virtualFile)
        .chooseTarget(editor)
        ?.toShortString(psiFile.project)
    }
  }

  private fun StarlarkCallExpression.calculateTargetId(): String? {
    val targetName = getTargetName() ?: return null
    val containingFile = containingFile?.virtualFile?.toNioPath() ?: return null
    return calculateLabel(project, containingFile, targetName)?.toShortString(project)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = shouldBeEnabledAndVisible(project, e)
  }

  private fun shouldBeEnabledAndVisible(project: Project, e: AnActionEvent): Boolean {
    if (!ALLOWED_ACTION_PLACES.contains(e.place)) return false
    val file = e.getPsiFile() ?: return false
    return if (file is StarlarkFile) {
      shouldAddActionToStarlarkFile(file, e)
    } else {
      file.virtualFile?.let { virtualFile ->
        project.targetUtils.getTargetsForFile(virtualFile).isNotEmpty()
      } ?: false
    }
  }

  private fun shouldAddActionToStarlarkFile(file: StarlarkFile, e: AnActionEvent): Boolean =
    file.isBuildFile() && shouldAddActionToElement(e.getPsiElement())

  private fun shouldAddActionToElement(psiElement: PsiElement?): Boolean =
    psiElement != null &&
      (psiElement.isValidTarget() || psiElement.isNameArgumentOfValidTarget())

  private fun AnActionEvent.getPsiElement(): PsiElement? {
    val psiFile = getPsiFile()
    return getData(CommonDataKeys.CARET)?.offset?.let { psiFile?.findElementAt(it) }
  }

  private fun PsiElement.isValidTarget(): Boolean =
    this.elementType == StarlarkTokenTypes.IDENTIFIER &&
      isPartOfTopLevelCallExpression()

  private fun PsiElement.isNameArgumentOfValidTarget(): Boolean {
    val starlarkStringLiteralExpression = this.parent
    val starlarkNamedArgumentExpression = starlarkStringLiteralExpression?.parent ?: return false
    return starlarkNamedArgumentExpression is StarlarkNamedArgumentExpression &&
      starlarkNamedArgumentExpression.isNameArgument() &&
      starlarkNamedArgumentExpression.isPartOfTopLevelCallExpression()
  }

  private fun PsiElement.isPartOfTopLevelCallExpression(): Boolean {
    val starlarkReferenceExpression = this.parent
    val starlarkCallExpression = starlarkReferenceExpression?.parent ?: return false
    return starlarkCallExpression is StarlarkCallExpression &&
      isTopLevelCall(starlarkCallExpression) &&
      starlarkCallExpression.getTargetName() != null
  }

  private fun isTopLevelCall(element: PsiElement): Boolean =
    element.parent is StarlarkExpressionStatement && element.parent?.parent is StarlarkFile
}

private val ALLOWED_ACTION_PLACES =
  listOf(
    ActionPlaces.EDITOR_POPUP,
    ActionPlaces.KEYBOARD_SHORTCUT,
    ActionPlaces.ACTION_SEARCH,
  )
