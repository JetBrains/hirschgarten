package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.ide.BrowserUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.references.BazelLabelReference
import org.jetbrains.bazel.languages.starlark.references.StarlarkLoadReference

internal class StarlarkLoadAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element.isLoadedSymbolLiteral() && isNotResolvable(element)) {
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.unresolved.reference", element.text),
      )
    } else if (element.isLoadFilenameLiteral() && isTooLargeLoadedFile(element)) {
      holder.annotateInfoWithFix(
          element = element,
          message = StarlarkBundle.message("annotator.too.large.file"),
          fix = OpenCodeInsightFileSizeHelpAction()
      )
    }
  }

  private fun isNotResolvable(element: PsiElement): Boolean {
    val reference = element.reference ?: return true
    val loadReference = reference as? StarlarkLoadReference ?: return false
    return loadReference.loadedFileReference.resolve() is StarlarkFile && !reference.isSoft && reference.resolve() == null
  }

  private fun PsiElement.isLoadedSymbolLiteral(): Boolean =
    elementType == StarlarkElementTypes.STRING_LITERAL_EXPRESSION &&
    (parent.elementType == StarlarkElementTypes.STRING_LOAD_VALUE || parent.elementType == StarlarkElementTypes.NAMED_LOAD_VALUE)

  private fun isTooLargeLoadedFile(element: PsiElement): Boolean {
    val reference = element.reference as? BazelLabelReference ?: return false
    val loadedFile = reference.resolve() as? PsiFile ?: return false
    val virtualFile = loadedFile.virtualFile ?: return false
    return loadedFile !is StarlarkFile && SingleRootFileViewProvider.isTooLargeForIntelligence(virtualFile)
  }

  private fun PsiElement.isLoadFilenameLiteral(): Boolean =
    elementType == StarlarkElementTypes.STRING_LITERAL_EXPRESSION &&
    parent is StarlarkFilenameLoadValue

  private class OpenCodeInsightFileSizeHelpAction : IntentionAction {
    override fun getText(): String = StarlarkBundle.message("quickfix.open.code.insight.file.size.help")

    override fun getFamilyName(): String = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
      BrowserUtil.browse("https://www.jetbrains.com/help/idea/tuning-the-ide.html#-v6psi5_124")
    }

    override fun startInWriteAction(): Boolean = false
  }
}
