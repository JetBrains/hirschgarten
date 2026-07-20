package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview.javaSROPatterns
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import java.nio.file.Path

internal class JavaProjectViewSourceRootPatternContributor : JavaSourceRootPatternContributor {

  override fun getPatterns(project: Project): JavaSourceRootPatterns {
    val projectView = project.projectView()
    // excludes have `-` before
    val (excludes, includes) = projectView.javaSROPatterns.partition { it.startsWith("-") }
    val rootDir = project.rootDir.toNioPath()
    return JavaSourceRootPatterns(
      includes = listOf(ProjectViewGlobPattern(rootDir, includes)),
      excludes = listOf(
        ProjectViewGlobPattern(
          rootDir = rootDir,
          // remove `-` before pattern
          patterns = excludes.map { it.substring(1) },
        ),
      ),
    )
  }
}

@ApiStatus.Internal
data class ProjectViewGlobPattern(val rootDir: Path, val patterns: List<String>) : SourceRootPattern {
  @Transient
  private var globSet: ProjectViewGlobSet? = null

  private fun globSet(): ProjectViewGlobSet = globSet ?: ProjectViewGlobSet(rootDir, patterns).also { globSet = it }

  override fun matches(root: Path): Boolean = globSet().matches(root)
}
