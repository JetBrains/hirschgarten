package org.jetbrains.bazel.languages.projectview.checker

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

@ApiStatus.Internal
fun interface ProjectViewChecker<T> {

  fun check(project: Project, subject: T): Sequence<ProjectViewProblem>

  companion object {

    private val sectionCheckers = sequenceOf(
      unsupportedSectionChecker,
      typeValueChecker,
      sectionDeprecationChecker,
    )

    val Main: ProjectViewChecker<PsiElement> = ProjectViewChecker { project, subject ->
      when (subject) {
        is ProjectViewPsiSection -> sectionCheckers
          .flatMap { it.check(project, subject) }
        else -> emptySequence()
      }
    }
  }
}

internal fun <T> projectViewChecker(
  block: suspend SequenceScope<ProjectViewProblem>.(project: Project, subject: T) -> Unit,
): ProjectViewChecker<T> = ProjectViewChecker { project, subject -> sequence { block(project, subject) } }

private val unsupportedSectionChecker = projectViewChecker<ProjectViewPsiSection> { _, subject ->
  if (ProjectViewSection.findByPsi(subject) == null) {
    val problem = ProjectViewProblem(
      id = ProjectViewProblem.Id.UnsupportedSection,
      message = BazelProjectViewBundle.message("annotator.unsupported.section.warning"),
      severity = ProjectViewProblem.Severity.Warning,
      element = subject.getKeyword(),
    )
    yield(problem)
  }
}

private val typeValueChecker = ProjectViewChecker<ProjectViewPsiSection> { project, subject ->
  val valueChecker = ProjectViewSection.findByPsi(subject)?.type?.valueChecker ?: return@ProjectViewChecker emptySequence()
  subject
    .getItems()
    .asSequence()
    .flatMap { item -> valueChecker.check(project, item.text.trim()).withElement(item) }
}

private val sectionDeprecationChecker = projectViewChecker<ProjectViewPsiSection> { _, subject ->
  val deprecation = ProjectViewSection.findByPsi(subject)?.deprecation ?: return@projectViewChecker
  yield(
    ProjectViewProblem(
      id = ProjectViewProblem.Id.Deprecation,
      message = deprecation.message,
      severity = deprecation.level.toSeverity(),
      element = subject.getKeyword(),
      quickFixes = deprecation.quickFixes
    )
  )
}

private fun ProjectViewSection.Deprecation.Level.toSeverity() = when (this) {
  ProjectViewSection.Deprecation.Level.Warning -> ProjectViewProblem.Severity.WeakWarning
  ProjectViewSection.Deprecation.Level.Error -> ProjectViewProblem.Severity.Error
}

internal fun <T : PsiElement> ProjectViewChecker<T>.check(element: T): Sequence<ProjectViewProblem> = check(element.project, element)

internal fun ProjectViewChecker<PsiElement>.checkChildren(file: ProjectViewPsiFile): Sequence<ProjectViewProblem> = file
  .children
  .asSequence()
  .flatMap { check(it) }

private fun Sequence<ProjectViewProblem>.withElement(element: PsiElement): Sequence<ProjectViewProblem> = map {
  when {
    it.element == null -> it.copy(element = element)
    else -> it
  }
}
