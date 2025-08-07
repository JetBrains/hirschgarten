package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentElement
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix

class StarlarkGlobAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is StarlarkGlobExpression && !element.isGlobValid()) {
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.glob.empty"),
        fixList =
          listOf(
            StarlarkGlobAllowEmptyQuickFix(element).asIntention(),
          ),
      )
    } else if (element is StarlarkStringLiteralExpression) {
      val glob = findGlob(element) ?: return
      if (glob.isPatternValid(element)) return
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.glob.empty.pattern"),
        fixList =
          listOf(
            StarlarkGlobAllowEmptyQuickFix(glob).asIntention(),
          ),
      )
    }
  }

  private fun findGlob(element: StarlarkStringLiteralExpression): StarlarkGlobExpression? {
    val listExpr = element.parent
    if (listExpr !is StarlarkListLiteralExpression) return null

    val argExpr = listExpr.parent
    if (argExpr !is StarlarkArgumentElement) return null

    val argList = argExpr.parent
    if (argList !is StarlarkArgumentList) return null

    val callExpr = argList.parent
    if (callExpr !is StarlarkCallExpression) return null

    val glob = callExpr.firstChild
    if (glob !is StarlarkGlobExpression) return null

    return glob
  }
}
