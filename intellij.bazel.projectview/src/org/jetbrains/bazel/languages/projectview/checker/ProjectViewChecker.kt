package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

typealias ProjectViewPsiSectionChecker = ProjectViewChecker<ProjectViewPsiSection>
typealias ProjectViewValueChecker = ProjectViewChecker<String>

@ApiStatus.Internal
fun interface ProjectViewChecker<T> {

  fun check(project: Project, subject: T, sink: Sink)

  fun interface Sink {
    fun report(problem: ProjectViewProblem)
  }

  companion object {

    private val checkers = listOf(
      unsupportedSectionChecker,
      typeValueChecker,
      sectionDeprecationChecker,
    )

    fun run(section: ProjectViewPsiSection, sink: Sink) {
      checkers.forEach {
        it.check(section.project, section, sink)
      }
    }
  }
}

private val unsupportedSectionChecker = ProjectViewPsiSectionChecker { _, subject, sink ->
  val section = ProjectViewSection.findByPsi(subject)
  if (section == null) {
    val problem = ProjectViewProblem(
      message = BazelProjectViewBundle.message("annotator.unsupported.section.warning"),
      severity = ProjectViewProblem.Severity.Warning,
      element = subject.getKeyword(),
    )
    sink.report(problem)
    return@ProjectViewPsiSectionChecker
  }
}

private val typeValueChecker = ProjectViewPsiSectionChecker { project, subject, sink ->
  val section = ProjectViewSection.findByPsi(subject) ?: return@ProjectViewPsiSectionChecker
  val type = section.type
  subject.getItems().forEach {
    type.valueChecker?.check(project, it.text.trim(), sink.withElement(it))
  }
}

private val sectionDeprecationChecker = ProjectViewPsiSectionChecker { _, subject, sink ->
  val section = ProjectViewSection.findByPsi(subject) ?: return@ProjectViewPsiSectionChecker
  val deprecation = section.deprecation ?: return@ProjectViewPsiSectionChecker
  sink.report(
    ProjectViewProblem(
      message = deprecation.message,
      severity = deprecation.level.toSeverity(),
      type = ProjectViewProblem.Type.Deprecation,
      element = subject.getKeyword(),
      quickFixes = deprecation.quickFixes
    )
  )
}

private fun ProjectViewSection.Deprecation.Level.toSeverity() = when (this) {
  ProjectViewSection.Deprecation.Level.Warning -> ProjectViewProblem.Severity.WeakWarning
  ProjectViewSection.Deprecation.Level.Error -> ProjectViewProblem.Severity.Error
}

private fun ProjectViewChecker.Sink.withElement(element: PsiElement): ProjectViewChecker.Sink {
  val delegate = this
  return ProjectViewChecker.Sink {
    val problem = when {
      it.element == null -> it.copy(element = element)
      else -> it
    }
    delegate.report(problem)
  }
}
