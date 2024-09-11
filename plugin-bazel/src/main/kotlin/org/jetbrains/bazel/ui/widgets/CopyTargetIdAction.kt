package org.jetbrains.bazel.ui.widgets

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.readAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.findParentOfType
import com.intellij.util.ui.TextTransferable
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir

// TODO: https://youtrack.jetbrains.com/issue/BAZEL-1158

/**
 Action is available in a BUILD file in the right click Editor Popup Menu under Copy/Paste Special
 while pressing on rule's name or value of the name argument of that rule
 */
internal class CopyTargetIdAction : SuspendableAction({ BazelPluginBundle.message("editor.action.copy.target.id") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val psiElement = readAction { e.getPsiElement() }
    performCopyTargetIdAction(psiElement)
  }

  private fun performCopyTargetIdAction(psiElement: PsiElement?) {
    val starlarkCallExpression = psiElement?.findParentOfType<StarlarkCallExpression>() ?: return
    val targetId = starlarkCallExpression.calculateTargetId() ?: return
    val clipboard = CopyPasteManager.getInstance()
    val transferable = TextTransferable(targetId as CharSequence)
    clipboard.setContents(transferable)
  }

  private fun StarlarkCallExpression.calculateTargetId(): String? {
    val targetName = getTargetName() ?: return null
    val projectBaseDirectory = project.rootDir
    val targetBaseDirectory = containingFile?.virtualFile?.parent ?: return null
    val relativeTargetBaseDirectory = VfsUtilCore.getRelativePath(targetBaseDirectory, projectBaseDirectory) ?: return null

    return "@//$relativeTargetBaseDirectory:$targetName"
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabledAndVisible =
      e.place == ActionPlaces.EDITOR_POPUP &&
      shouldAddActionToFile(project, e) &&
      shouldAddActionToElement(e)
  }

  private fun shouldAddActionToFile(project: Project, e: AnActionEvent): Boolean {
    val starlarkFile = e.getPsiFile() as? StarlarkFile
    return project.isBspProject &&
      starlarkFile != null &&
      starlarkFile.project == project &&
      starlarkFile.isBuildFile()
  }

  private fun shouldAddActionToElement(e: AnActionEvent): Boolean {
    val psiElement = e.getPsiElement()
    return psiElement != null &&
      (psiElement.isValidTarget() || psiElement.isNameArgumentOfValidTarget())
  }

  private fun AnActionEvent.getPsiElement(): PsiElement? {
    val psiFile = getPsiFile()
    return getData(CommonDataKeys.CARET)?.offset?.let { psiFile?.findElementAt(it) }
  }

  private fun AnActionEvent.getPsiFile(): PsiElement? = CommonDataKeys.PSI_FILE.getData(dataContext)

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
