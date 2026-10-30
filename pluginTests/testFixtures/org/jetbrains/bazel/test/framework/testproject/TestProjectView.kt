package org.jetbrains.bazel.test.framework.testproject

import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import java.nio.file.Path

fun TestBazelProject.parseProjectView(source: Path): ProjectView = ProjectViewFactory.from(project, source)

fun TestBazelProject.parseProjectView(@Language("projectview") content: String, source: Path? = null): ProjectView =
  ProjectViewFactory.from(project, content, source = source)
