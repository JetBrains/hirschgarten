package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.modcommand.ModCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.ProjectViewLocalQuickFix
import org.jetbrains.bazel.languages.projectview.annotation.quickfix.quickFixActions
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewActionWithSuppressOptions
import org.jetbrains.bazel.languages.projectview.annotation.suppress.ProjectViewSuppressOptionsAction
import org.jetbrains.bazel.languages.projectview.annotation.suppress.suppressActions
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewChecker
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import org.jetbrains.bazel.languages.projectview.checker.check
import org.jetbrains.bazel.languages.projectview.checker.isSuppressible

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

  private fun AnnotationBuilder.withQuickFixesFrom(problem: ProjectViewProblem): AnnotationBuilder {
    val suppressActions = if (problem.isSuppressible) problem.id.suppressActions() else emptyList()
    val fixesActions = when {
      problem.quickFixes.isEmpty() && suppressActions.isNotEmpty() -> listOf(ProjectViewSuppressOptionsAction(problem.id, suppressActions))
      else -> problem.quickFixActions().map { ProjectViewLocalQuickFix(it, suppressActions) }
    }
    return fixesActions.fold(this) { builder, entry ->
      builder
        .newFix(entry)
        .withSuppressOptionsKey(suppressActions)
        .universalIfQuickFix(entry)
        .registerFix()
    }
  }

  /**
   * The display name of the key is the title of the submenu that holds the suppress actions.
   * An annotator cannot give a fix its own title, so without this key the title is the name of the annotator tool, "Annotator".
   */
  private fun AnnotationBuilder.FixBuilder.withSuppressOptionsKey(suppressActions: List<ModCommandAction>) = when {
    suppressActions.isEmpty() -> this
    else -> key(ProjectViewActionWithSuppressOptions.KEY)
  }

  private fun AnnotationBuilder.FixBuilder.universalIfQuickFix(entry: IntentionAction) = when (entry) {
    is LocalQuickFix -> universal()
    else -> this
  }

  private fun AnnotationBuilder.highlightingTypeFor(problem: ProjectViewProblem) = when (problem.id) {
    ProjectViewProblem.Id.Deprecation -> highlightType(ProblemHighlightType.LIKE_DEPRECATED)
    else -> this
  }
}
