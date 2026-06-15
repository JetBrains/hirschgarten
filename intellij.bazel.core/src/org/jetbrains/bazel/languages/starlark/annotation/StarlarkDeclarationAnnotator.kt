package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.codeInsight.daemon.SyntheticPsiFileSupport
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.elementType
import com.intellij.ui.JBColor
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.index.StarlarkLoadsIndex
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTupleExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkParenthesizedExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameter
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkVariadicParameter
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue
import java.awt.Font

private val UNUSED_TEXT_ATTRIBUTES = TextAttributes(JBColor.GRAY, null, null, null, Font.PLAIN)

internal class StarlarkDeclarationAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element.isUnusedDeclaration()) {
      holder.annotateUnused(element, (element.parent as? StarlarkNamedElement)?.name ?: "")
    }
  }

  private fun PsiElement.isUnusedDeclaration(): Boolean {
    if (!isDeclaration()) return false
    val parent = this.parent
    // Not a variable — unused if it does not refer to anything and is also unreferenced.
    if (parent !is StarlarkTargetExpression) return parent.reference?.resolve() == null && parent.isUnreferenced()
    // If a variable is not inside assignment, we consider it a fresh declaration
    if (!parent.isInsideAssignStatement()) return parent.isUnreferenced()
    val originalAssignment = parent.reference.resolve() ?: return parent.isUnreferenced()
    // A reassignment is unused only when the canonical (first) binding itself is
    // unreferenced — i.e. nothing besides other reassignments refers to it.
    return originalAssignment.isUnreferenced()
  }

  private fun PsiElement.isDeclaration(): Boolean =
    elementType == StarlarkTokenTypes.IDENTIFIER &&
    parent is StarlarkNamedElement &&
    parent !is StarlarkVariadicParameter &&
    parent !is StarlarkKeywordVariadicParameter

  private fun PsiElement.isUnreferenced(): Boolean {
    if (shouldSkipUnreferencedCheck()) return false
    val scope = useScope
    if (scope is GlobalSearchScope && this is StarlarkNamedElement && StarlarkLoadsIndex.isLoaded(this, scope)) return false
    // if not accessed externally, check for local usages
    val limitedScope = scope.intersectWith(LocalSearchScope(this.containingFile))
    val references = ReferencesSearch.search(this, limitedScope)
    if (this !is StarlarkTargetExpression) return references.none()
    // Variable is unreferenced if only reassignments refer to it.
    return references.none { it.element !is StarlarkTargetExpression }
  }

  private fun PsiElement.shouldSkipUnreferencedCheck(): Boolean {
    if (!isStarlarkTopLevel() && isExplicitlyUnused()) return true
    // it should be possible to always check local declarations usage
    if (useScope !is GlobalSearchScope) return false
    return DumbService.isDumb(project) || SyntheticPsiFileSupport.isOutsiderFile(containingFile.virtualFile)
  }

  // In Starlark, _-prefixed names signal intentionally unused declarations (like Python's _ convention).
  private fun PsiElement.isExplicitlyUnused() = when (this) {
    is StarlarkNamedElement -> name?.startsWith("_") == true
    else -> false
  }

  private fun PsiElement.isStarlarkTopLevel() = when (this) {
    is StarlarkTargetExpression -> isTopLevel()
    is StarlarkFunctionDeclaration -> isTopLevel()
    else -> false
  }

  private fun StarlarkTargetExpression.isInsideAssignStatement(): Boolean {
    var current: PsiElement = this
    while (true) {
      when (current.parent) {
        is StarlarkAssignmentStatement -> return true
        is StarlarkTupleExpression, is StarlarkParenthesizedExpression -> current = current.parent
        else -> return false
      }
    }
  }

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
