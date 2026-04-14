package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview.javaSROPatterns
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet

private class JavaProjectViewSourceRootPatternContributor : JavaSourceRootPatternContributor {

  override fun getPatterns(project: Project): JavaSourceRootPatterns {
    val projectView = project.projectView()
    // excludes have `-` before
    val (excludes, includes) = projectView.javaSROPatterns.partition { it.startsWith("-") }
    val rootDir = project.rootDir.toNioPath()
    return JavaSourceRootPatterns(
      includes = listOf(ProjectViewGlobSet(rootDir, includes).toSourceRootPattern()),
      excludes = listOf(
        ProjectViewGlobSet(
          rootDir = rootDir,
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
