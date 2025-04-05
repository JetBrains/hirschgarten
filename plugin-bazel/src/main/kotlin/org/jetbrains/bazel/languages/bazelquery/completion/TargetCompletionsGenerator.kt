package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.execution.configurations.ParameterTargetValuePart.Path
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils
import kotlin.text.removePrefix
import kotlin.text.startsWith

class TargetCompletionsGenerator(private val project: Project) {
  private val separator = if (SystemInfo.isWindows) "\\" else "/"
  private val startPathSign = "$separator$separator"
  private val allTargets =
    project.targetUtils
      .allTargets()
      .map { it.toShortString(project) }
      .toMutableSet()

  fun getTargetsList(prefix: String, directory: Path? = null): List<String> {
    // Note: project root dependent format - starts with "//",
    // directory dependent format - starts with a letter or ":"
    val suggestions = mutableListOf<String>()
    val projectDir = project.basePath ?: return suggestions
    if (directory != null && !directory.pathToUpload.startsWith(projectDir)) return suggestions
    val currentDir = directory?.pathToUpload?.removePrefix(projectDir)?.removePrefix(separator) ?: ""
    val prefixPath = "$startPathSign$currentDir"

    val allTargetsDirDepFormat =
      if (currentDir.isNotEmpty()) {
        allTargets
          .filter { it.startsWith("$prefixPath$separator") }
          .map { it.removePrefix("$prefixPath$separator") }
      } else {
        allTargets.map { it.removePrefix(startPathSign) }
      }
    allTargets.addAll(allTargetsDirDepFormat)

    suggestions.addAll(allTargets.filter { it.startsWith(prefix) })
    suggestions.addAll(generateTargetPatterns(prefix, suggestions))

    if (prefix.startsWith(separator) || prefix.isEmpty()) {
      // All rule targets in packages in the main repository. Does not include targets from external repositories.
      suggestions.add("$startPathSign...")
      // All rule targets in the top-level package, if there is a `BUILD` file at the root of the workspace.
      suggestions.add("$startPathSign:all")
    }
    if (prefix.startsWith(":") || prefix.isEmpty()) {
      // All rule targets in the working directory.
      suggestions.add(":all")
      if (prefix.isNotEmpty()) {
        // Targets in the working directory in directory dependent format (start with ":$prefix")
        suggestions.addAll(
          allTargets
            .filter { it.startsWith("$prefixPath$prefix") }
            .map { it.removePrefix(prefixPath) },
        )
      } else {
        // Targets in the working directory in directory dependent format (start with ":")
        suggestions.addAll(
          allTargets
            .filter { it.startsWith("$prefixPath:") }
            .map { it.removePrefix(prefixPath) },
        )
      }
    }
    if (prefix.startsWith(".") || prefix.isEmpty()) {
      // All rule targets in all packages beneath the working directory.
      suggestions.add("...:all")
    }
    // Note: The following are not supported in completions:
    // bar/wiz - Equivalent to (assuming foo is current working directory):
    //    foo/bar/wiz:wiz if foo/bar/wiz is a package
    //    foo/bar:wiz if foo/bar is a package
    //    foo:bar/wiz otherwise

    return suggestions.distinct()
  }

  private fun generateTargetPatterns(prefix: String, suggestions: List<String>): List<String> {
    val patterns = mutableListOf<String>()
    val availableSuggestions = suggestions.toMutableList()
    val prefixSegments =
      if (prefix.isEmpty()) {
        0
      } else {
        prefix
          .removePrefix(separator)
          .removePrefix(separator)
          .split(":")[0]
          .split(separator)
          .size
      }

    availableSuggestions
      .forEach { s ->
        patterns.add(s) // add single target suggestion
        val isProjectRootDependentFormat = s.startsWith(startPathSign)
        val suggestionSegments = s.removePrefix(startPathSign).split(":")[0].split(separator)
        suggestionSegments.indices
          .reversed()
          .filter { it + 1 >= prefixSegments }
          .forEach { i ->
            // add wildcard patterns for the subpath of target suggestion
            if (isProjectRootDependentFormat) {
              val path = startPathSign + suggestionSegments.subList(0, i + 1).joinToString(separator)
              patterns.add("$path:all")
              patterns.add("$path$separator...:all-targets")
              patterns.add("$path$separator...:all")
            } else {
              val path = suggestionSegments.subList(0, i + 1).joinToString(separator)
              patterns.add("$path:all")
              patterns.add("$path$separator...:all")
            }
          }
      }

    return patterns
  }
}
