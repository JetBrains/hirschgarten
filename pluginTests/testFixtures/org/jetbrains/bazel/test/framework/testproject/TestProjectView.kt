package org.jetbrains.bazel.test.framework.testproject

import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem
import java.nio.file.Path

fun TestBazelProject.parseProjectView(source: Path, onEachProblem: ((ProjectViewProblem) -> Unit)? = null): ProjectView =
  ProjectViewFactory.from(project, source, onEachProblem = onEachProblem)

fun TestBazelProject.parseProjectView(
  @Language("projectview") content: String,
  source: Path? = null,
  onEachProblem: ((ProjectViewProblem) -> Unit)? = null,
): ProjectView = ProjectViewFactory.from(project, content, source = source, onEachProblem = onEachProblem)
