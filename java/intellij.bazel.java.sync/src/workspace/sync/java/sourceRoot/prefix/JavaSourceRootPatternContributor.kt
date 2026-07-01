package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.prefix

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
fun interface SourceRootPattern {
  fun matches(root: Path): Boolean
}

@ApiStatus.Internal
interface JavaSourceRootPatternContributor {
  fun getPatterns(project: Project): JavaSourceRootPatterns

  companion object {
    val EP_NAME: ExtensionPointName<JavaSourceRootPatternContributor> = ExtensionPointName("org.jetbrains.bazel.javaSourceRootPrefixContributor")

    fun allSourceRootPatterns(project: Project): JavaSourceRootPatterns {
      val patterns = EP_NAME.extensionList.map { it.getPatterns(project) }
      return JavaSourceRootPatterns(
        includes = patterns.flatMap { it.includes },
        excludes = patterns.flatMap { it.excludes }
      )
    }
  }
}

@ApiStatus.Internal
data class JavaSourceRootPatterns(val includes: List<SourceRootPattern>, val excludes: List<SourceRootPattern>)
