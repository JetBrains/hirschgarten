package org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview.javaSROExcludePatterns
import org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview.javaSROIncludePatterns
import java.nio.file.FileSystems
import java.nio.file.Path

private class JavaProjectViewSourceRootPatternContributor : JavaSourceRootPatternContributor {
  override fun getIncludePatterns(project: Project): List<SourceRootPattern> = getSourceRootPattern(project) { it.javaSROIncludePatterns }

  override fun getExcludePatterns(project: Project): List<SourceRootPattern> = getSourceRootPattern(project) { it.javaSROExcludePatterns }

  private fun getSourceRootPattern(
    project: Project,
    func: (projectview: ProjectView) -> List<String>,
  ): List<SourceRootPattern> {
    val projectView = ProjectViewService.getInstance(project).getCachedProjectView()
    return func(projectView).map { pattern ->
      val matcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
      object : SourceRootPattern {
        override fun invoke(p1: String): Boolean = matcher.matches(Path.of(p1))
      }
    }
  }
}
