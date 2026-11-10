package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlobCall
import org.jetbrains.bazel.languages.starlark.globbing.asGlobCallOrNull
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix

internal class StarlarkGlobAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val glob = (element as? StarlarkCallExpression)?.asGlobCallOrNull()
    if (glob != null && !isGlobValid(glob)) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, StarlarkBundle.message("annotator.glob.empty"))
        .range(glob.calledExpression)
        .withFix(StarlarkGlobAllowEmptyQuickFix(glob.call))
        .create()
    } else if (element is StarlarkStringLiteralExpression) {
      val containingGlob = element.parentOfType<StarlarkCallExpression>()?.asGlobCallOrNull() ?: return
      if (isPatternValid(containingGlob, element)) return
      holder
        .newAnnotation(HighlightSeverity.WARNING, StarlarkBundle.message("annotator.glob.empty.pattern"))
        .range(element)
        .withFix(StarlarkGlobAllowEmptyQuickFix(containingGlob.call))
        .create()
    }
  }

  /**
   * The pattern is valid if matches at least one file
   * or the 'allow_empty' argument is set to true.
   */
  private fun isPatternValid(glob: StarlarkGlobCall, element: StarlarkStringLiteralExpression): Boolean {
    if (glob.isEmptyAllowed()) return true
    val pattern = element.getStringContents()
    val containingDirectory = glob.call.containingFile.parent?.virtualFile ?: return true
    return try {
      StarlarkGlob
        .forPath(containingDirectory)
        .addPattern(pattern)
        .build()
        .execute()
        .isNotEmpty()
    } catch (_: IllegalArgumentException) {
      true
    }
  }

  /**
   * The glob is valid if it resolves to a non-empty set of files,
   * or the 'allow_empty' argument is set to true.
   */
  private fun isGlobValid(glob: StarlarkGlobCall): Boolean {
    if (glob.isEmptyAllowed()) return true
    val matchedFiles = (glob.calledExpression.reference as? PsiPolyVariantReference)?.multiResolve(false) ?: return true
    return matchedFiles.isNotEmpty()
  }
}
