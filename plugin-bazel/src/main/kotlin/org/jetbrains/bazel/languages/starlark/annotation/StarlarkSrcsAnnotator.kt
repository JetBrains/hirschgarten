package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkCreateFileQuickFix
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix

class StarlarkSrcsAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is StarlarkGlobExpression && !element.isGlobValid()) {
      holder.annotateError(
        element = element,
        message = StarlarkBundle.message("annotator.srcs.glob.empty"),
        fixList =
          listOf(
            StarlarkGlobAllowEmptyQuickFix(element).asIntention(),
          ),
      )
    } else if (element.parent.firstChild.text == "srcs" && element is StarlarkListLiteralExpression) {
      for (child in element.children) {
        if (child is StarlarkStringLiteralExpression && !child.isResolvableToFile()) {
          holder.annotateError(
            element = child,
            message = StarlarkBundle.message("annotator.srcs.nonexisting.file"),
            fixList =
              listOf(
                StarlarkCreateFileQuickFix(child).asIntention(),
              ),
          )
        }
      }
    }
  }
}
