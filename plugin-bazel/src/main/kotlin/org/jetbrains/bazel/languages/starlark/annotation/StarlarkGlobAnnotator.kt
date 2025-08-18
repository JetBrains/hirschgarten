package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix

class StarlarkGlobAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is StarlarkGlobExpression && !element.isGlobValid()) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, StarlarkBundle.message("annotator.glob.empty"))
        .range(element)
        .withFix(StarlarkGlobAllowEmptyQuickFix(element))
        .create()
    } else if (element is StarlarkStringLiteralExpression) {
      val glob = findGlob(element) ?: return
      if (glob.isPatternValid(element)) return
      holder
        .newAnnotation(HighlightSeverity.WARNING, StarlarkBundle.message("annotator.glob.empty.pattern"))
        .range(element)
        .withFix(StarlarkGlobAllowEmptyQuickFix(glob))
        .create()
    }
  }

  private fun findGlob(element: StarlarkStringLiteralExpression): StarlarkGlobExpression? {
    val callExpr = element.parentOfType<StarlarkCallExpression>() ?: return null
    return callExpr.firstChild as? StarlarkGlobExpression
  }
}
