package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.codeInsight.intention.CommonIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.MergeIntoSectionQuickFix
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.ReplaceWithIndexQuickFix
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

@Suppress("SplitModeApiUsage")
internal class ProjectViewProblemsAnnotator : Annotator, DumbAware {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is ProjectViewPsiSection) return
    val sink = ProjectViewChecker.Sink { problem ->
      holder.newAnnotation(problem.severity.toHighlightSeverity(), problem.message)
        .rangeIfPresent(problem.element)
        .withQuickFixesFrom(problem)
        .highlightingTypeFor(problem)
        .create()
    }
    ProjectViewChecker.run(element, sink)
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

  private fun AnnotationBuilder.withQuickFixesFrom(problem: ProjectViewProblem): AnnotationBuilder {
    if (problem.quickFixes.isEmpty()) return this
    return problem
      .quickFixes
      .mapNotNull { it.toIntentionAction(problem.element) }
      .fold(this) { builder, fix -> builder.withFix(fix) }
  }

  private fun ProjectViewProblem.QuickFix.toIntentionAction(element: PsiElement?): CommonIntentionAction? {
    val section = element?.parentOfType<ProjectViewPsiSection>(withSelf = true) ?: return null
    return when (this) {
      is ProjectViewProblem.QuickFix.MergeIntoSection -> MergeIntoSectionQuickFix(section, this.targetSectionKey)
      ProjectViewProblem.QuickFix.ReplaceWithIndex -> ReplaceWithIndexQuickFix(section)
    }
  }

  private fun AnnotationBuilder.highlightingTypeFor(problem: ProjectViewProblem) = when (problem.type) {
    ProjectViewProblem.Type.Message -> this
    ProjectViewProblem.Type.Deprecation -> highlightType(ProblemHighlightType.LIKE_DEPRECATED)
  }
}
