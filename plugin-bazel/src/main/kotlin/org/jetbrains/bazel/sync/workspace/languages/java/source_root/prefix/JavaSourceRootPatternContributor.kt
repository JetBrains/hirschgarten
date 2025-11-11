package org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

typealias SourceRootPattern = (root: String) -> Boolean

interface JavaSourceRootPatternContributor {
  fun getPatterns(project: Project): JavaSourceRootPatterns

  companion object {
    val ep = ExtensionPointName<JavaSourceRootPatternContributor>("org.jetbrains.bazel.javaSourceRootPrefixContributor")
  }
}

data class JavaSourceRootPatterns(val includes: List<SourceRootPattern>, val excludes: List<SourceRootPattern>)
