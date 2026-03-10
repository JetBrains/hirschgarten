package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
fun interface SourceRootPattern {
  fun matches(root: String): Boolean
}

@ApiStatus.Internal
interface JavaSourceRootPatternContributor {
  fun getPatterns(project: Project): JavaSourceRootPatterns

  companion object {
    val ep = ExtensionPointName<JavaSourceRootPatternContributor>("org.jetbrains.bazel.javaSourceRootPrefixContributor")
  }
}

@ApiStatus.Internal
data class JavaSourceRootPatterns(val includes: List<SourceRootPattern>, val excludes: List<SourceRootPattern>)
