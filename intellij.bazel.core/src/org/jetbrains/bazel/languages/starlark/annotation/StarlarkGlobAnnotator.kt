package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.bazelversion.psi.toSemVer
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkConstants.ALLOW_EMPTY_KEYWORD
import org.jetbrains.bazel.languages.starlark.globbing.StarlarkGlob
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.quickFixes.StarlarkGlobAllowEmptyQuickFix

internal class StarlarkGlobAnnotator : StarlarkAnnotator() {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is StarlarkGlobExpression && !isGlobValid(element)) {
      holder
        .newAnnotation(HighlightSeverity.ERROR, StarlarkBundle.message("annotator.glob.empty"))
        .range(element)
        .withFix(StarlarkGlobAllowEmptyQuickFix(element))
        .create()
    } else if (element is StarlarkStringLiteralExpression) {
      val globExpression = findContainingGlobExpression(element) ?: return
      if (isPatternValid(globExpression, element)) return
      holder
        .newAnnotation(HighlightSeverity.WARNING, StarlarkBundle.message("annotator.glob.empty.pattern"))
        .range(element)
        .withFix(StarlarkGlobAllowEmptyQuickFix(globExpression))
        .create()
    }
  }

  private fun findContainingGlobExpression(element: StarlarkStringLiteralExpression): StarlarkGlobExpression? {
    val callExpr = element.parentOfType<StarlarkCallExpression>() ?: return null
    return callExpr.firstChild as? StarlarkGlobExpression
  }

  /**
   * The pattern is valid if matches at least one file
   * or the 'allow_empty' argument is set to true.
   */
  private fun isPatternValid(globExpression: StarlarkGlobExpression, element: StarlarkStringLiteralExpression): Boolean {
    if (isAllowedEmpty(globExpression)) return true
    val pattern = element.getStringContents()
    val containingDirectory = globExpression.containingFile.parent?.virtualFile ?: return true
    return try {
      StarlarkGlob
        .forPath(containingDirectory)
        .addPattern(pattern)
        .build()
        .execute()
        .isNotEmpty()
    } catch (e: IllegalArgumentException) {
      true
    }
  }

  /**
   * The glob is valid if it resolves to a non-empty set of files,
   * or the 'allow_empty' argument is set to true.
   */
  private fun isGlobValid(expr: StarlarkGlobExpression): Boolean {
    if (isAllowedEmpty(expr)) return true

    val matchedFiles = expr.reference.multiResolve(false)
    return matchedFiles.isNotEmpty()
  }

  private fun isAllowedEmpty(expr: StarlarkGlobExpression): Boolean {
    val allowEmpty: String? = expr.getKeywordArgument(ALLOW_EMPTY_KEYWORD)?.getValue()?.text
    return allowEmpty?.lowercase()?.toBooleanStrictOrNull() ?: run {
      val defaultAllowEmptyTrue =  expr.project.service<BazelVersionCheckerService>().currentBazelVersion?.toSemVer()?.major?.let { it < 8 } ?: false
      defaultAllowEmptyTrue
    }
  }
}
