package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.psi.impl.StarlarkStringLiteralExpressionImpl

private const val TRIPLE_QUOTE = "\"\"\""
private const val TRIPLE_APOSTROPHE = "\'\'\'"

class StarlarkStringAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is StarlarkStringLiteralExpressionImpl) {
      element.firstChild.text.annotateUnterminated(element, holder)
    }
  }

  private fun String.annotateUnterminated(element: PsiElement, holder: AnnotationHolder) = when {
    isTripleQuoteUnterminated() -> holder.annotate(
      element = element,
      message = StarlarkBundle.message("annotator.missing.closing.triple.quote")
    )

    isSingleQuoteUnterminated() -> holder.annotate(
      element = element,
      message = StarlarkBundle.message("annotator.missing.closing.quote", first())
    )

    else -> {}
  }

  private fun String.isTripleQuoted(): Boolean = startsWith(TRIPLE_QUOTE) || startsWith(TRIPLE_APOSTROPHE)

  private fun String.isSingleQuoteUnterminated(): Boolean =
    length == 1 || first() != last() || hasOddNumberOfPrecedingBackslashes()

  private fun String.isTripleQuoteUnterminated(): Boolean =
    isTripleQuoted() && (length < 6 || take(3) != takeLast(3))

  private fun String.hasOddNumberOfPrecedingBackslashes(): Boolean =
    dropLast(1).takeLastWhile { it == '\\' }.length % 2 == 1

  private fun AnnotationHolder.annotate(element: PsiElement, message: String) {
    newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
  }
}
