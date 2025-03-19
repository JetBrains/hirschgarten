package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.target.targetUtils
import kotlin.text.removePrefix
import kotlin.text.startsWith


private val separator = if (SystemInfo.isWindows) "\\" else "/"
private val startPathSign = "$separator$separator"

private val targets = ProjectManager.getInstance()
  .openProjects.firstOrNull()
  ?.targetUtils?.allTargets()?.map {
    it.toString().removePrefix("@")
  }

fun generateTargetCompletions(prefix: String, directory: String = ""): List<String> {
  // Note: project root dependent format - starts with "//",
  // directory dependent format - starts with a letter or ":"
  val suggestions = mutableListOf<String>()
  val projectDir = ProjectManager.getInstance().openProjects.firstOrNull()?.basePath ?: ""
  if (!directory.startsWith(projectDir) && directory.isNotEmpty()) return suggestions
  val currentDir = directory.removePrefix(projectDir).removePrefix(separator)
  val prefixPath = "$startPathSign$currentDir"

  val allTargets: MutableSet<String> =  targets?.toMutableSet() ?: mutableSetOf()
  val allTargetsDirDepFormat =
    if (currentDir.isNotEmpty())
      allTargets
        .filter { it.startsWith("$prefixPath$separator") }
        .map { it.removePrefix("$prefixPath$separator") }
    else
      allTargets.map{ it.removePrefix(startPathSign) }
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
    if(prefix.isNotEmpty())
      // Targets in the working directory in directory dependent format (start with ":$prefix")
      suggestions.addAll(
        allTargets
          .filter { it.startsWith("$prefixPath$prefix") }
          .map { it.removePrefix(prefixPath) }
      )
    else
      // Targets in the working directory in directory dependent format (start with ":")
      suggestions.addAll(
        allTargets
          .filter { it.startsWith("$prefixPath:") }
          .map { it.removePrefix(prefixPath) }
      )
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

  val availablePathsSet = mutableSetOf<String>()
  val availableSuggestions = suggestions.toMutableList()
  var isProjectRootDependentFormat = false
  var prefixSegments = 0

  if (prefix.isNotEmpty()) {
    isProjectRootDependentFormat = prefix[0] == separator[0]
    prefixSegments += prefix
      .removePrefix(separator).removePrefix(separator).split(":")[0]
      .split(separator).size
  }

  // Add all subpaths to use with wildcards
  availableSuggestions
    .forEach { s ->
      patterns.add(s)
      val suggestionSegments = s.removePrefix(startPathSign).split(":")[0].split(separator)
      suggestionSegments.indices.reversed()
        .filter { it + 1 >= prefixSegments }
        .forEach { i ->
          availablePathsSet.add(suggestionSegments.subList(0, i + 1).joinToString(separator))
        }
    }

  if (isProjectRootDependentFormat) {
    availablePathsSet.forEach { path ->
      patterns.add("$startPathSign$path:all")
      patterns.add("$startPathSign$path$separator...:all-targets")
      patterns.add("$startPathSign$path$separator...:all")
    }
  }
  else {
    availablePathsSet.forEach { path ->
      patterns.add("$path:all")
      patterns.add("$path$separator...:all")
    }
  }

  return patterns
}
