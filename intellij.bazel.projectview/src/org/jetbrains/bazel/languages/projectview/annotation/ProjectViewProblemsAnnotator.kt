package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.ProjectViewLocalQuickFix
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.quickFixActions
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.checker.check

@Suppress("SplitModeApiUsage")
internal class ProjectViewProblemsAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    ProjectViewChecker.Main.check(element).forEach { problem ->
      holder.newAnnotation(problem.severity.toHighlightSeverity(), problem.message)
        .rangeIfPresent(problem.element)
        .withQuickFixesFrom(problem)
        .highlightingTypeFor(problem)
        .create()
    }
  }

  private fun ProjectViewProblem.Severity.toHighlightSeverity() = when (this) {
    ProjectViewProblem.Severity.Warning -> HighlightSeverity.WARNING
    ProjectViewProblem.Severity.Error -> HighlightSeverity.ERROR
    ProjectViewProblem.Severity.WeakWarning -> HighlightSeverity.WEAK_WARNING
  }

  private fun AnnotationBuilder.rangeIfPresent(element: PsiElement?) = when {
    element == null -> this
    else -> range(element)
  }

  private fun AnnotationBuilder.withQuickFixesFrom(problem: ProjectViewProblem): AnnotationBuilder = problem
    .quickFixActions()
    .fold(this) { builder, fix ->
      builder
        .newFix(ProjectViewLocalQuickFix(fix))
        .universal()
        .registerFix()
    }

  private fun AnnotationBuilder.highlightingTypeFor(problem: ProjectViewProblem) = when (problem.id) {
    ProjectViewProblem.Id.Deprecation -> highlightType(ProblemHighlightType.LIKE_DEPRECATED)
    else -> this
  }
}
