package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
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
        .create()
    }
    ProjectViewChecker.run(element, sink)
  }

  private fun ProjectViewProblem.Severity.toHighlightSeverity() = when (this) {
    ProjectViewProblem.Severity.Warning -> HighlightSeverity.WARNING
    ProjectViewProblem.Severity.Error -> HighlightSeverity.ERROR
  }

  private fun AnnotationBuilder.rangeIfPresent(element: PsiElement?) = when {
    element == null -> this
    else -> range(element)
  }
}
