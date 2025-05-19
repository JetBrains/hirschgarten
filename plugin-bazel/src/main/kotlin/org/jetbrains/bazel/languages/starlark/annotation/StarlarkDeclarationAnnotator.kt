package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import java.awt.Font

private val UNUSED_TEXT_ATTRIBUTES = TextAttributes(JBColor.GRAY, null, null, null, Font.PLAIN)

class StarlarkDeclarationAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (isDeclaration(element) &&
      hasNoUsages(element.parent) &&
      isNotUsage(element)
    ) {
      holder.annotateUnused(element, (element.parent as? StarlarkNamedElement)?.name ?: "")
    }
  }

  private fun isDeclaration(element: PsiElement): Boolean =
    element.elementType == StarlarkTokenTypes.IDENTIFIER &&
      element.parent is StarlarkNamedElement &&
      element.parent !is StarlarkVariadicParameter &&
      element.parent !is StarlarkKeywordVariadicParameter

  private fun hasNoUsages(element: PsiElement): Boolean {
    val scope =
      if (element.isTopLevelTarget() || element.isTopLevelFunction()) {
        element.useScope
      } else {
        GlobalSearchScope.fileScope(element.containingFile)
      }
    return ReferencesSearch.search(element, scope).asIterable().none()
  }

  private fun PsiElement.isTopLevelTarget() = this is StarlarkTargetExpression && isTopLevel()

  private fun PsiElement.isTopLevelFunction() = this is StarlarkFunctionDeclaration && isTopLevel()

  private fun isNotUsage(element: PsiElement) = element.parent.reference?.resolve() == null

  private fun AnnotationHolder.annotateUnused(element: PsiElement, name: String) =
    newAnnotation(
      HighlightSeverity.WEAK_WARNING,
      StarlarkBundle.message("annotator.unused.declaration", declarationTypeOfNamedElement(element.parent), name),
    ).range(element).enforcedTextAttributes(UNUSED_TEXT_ATTRIBUTES).create()

  private fun declarationTypeOfNamedElement(element: PsiElement): String =
    when (element) {
      is StarlarkNamedLoadValue -> "Load value"
      is StarlarkFunctionDeclaration -> "Function"
      is StarlarkTargetExpression -> "Variable"
      is StarlarkParameter -> "Parameter"
      else -> ""
    }
}
