package org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview.javaSROPatterns
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import kotlin.text.startsWith

private class JavaProjectViewSourceRootPatternContributor : JavaSourceRootPatternContributor {

  override fun getPatterns(project: Project): JavaSourceRootPatterns {
    val projectView = ProjectViewService.getInstance(project).getCachedProjectView()
    // excludes have `-` before
    val (excludes, includes) = projectView.javaSROPatterns.partition { it.startsWith("-") }
    return JavaSourceRootPatterns(
      includes = listOf(ProjectViewGlobSet(includes).toSourceRootPattern()),
      excludes = listOf(
        ProjectViewGlobSet(
          // remove `-` before pattern
          patterns = excludes.map { it.substring(1) },
        ).toSourceRootPattern(),
      ),
    )
  }


  private fun ProjectViewGlobSet.toSourceRootPattern(): SourceRootPattern {
    return SourceRootPattern { path -> this.matches(path) }
  }
}
