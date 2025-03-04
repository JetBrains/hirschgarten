package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.openapi.project.ProjectManager
import org.jetbrains.bazel.target.targetUtils
import kotlin.text.removePrefix
import kotlin.text.startsWith


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
  val currentDir = directory.removePrefix(projectDir).removePrefix("/")
  val prefixPath = "//$currentDir"

  val allTargets: MutableSet<String> =  targets?.toMutableSet() ?: mutableSetOf()
  val allTargetsDirDepFormat =
    if (currentDir.isNotEmpty())
      allTargets
        .filter { it.startsWith("$prefixPath/") }
        .map { it.removePrefix("$prefixPath/") }
    else
      allTargets.map{ it.removePrefix("//") }
  allTargets.addAll(allTargetsDirDepFormat)

  suggestions.addAll(allTargets.filter { it.startsWith(prefix) })
  suggestions.addAll(generateTargetPatterns(prefix, suggestions))

  if (prefix.startsWith("/") || prefix.isEmpty()) {
    // All rule targets in packages in the main repository. Does not include targets from external repositories.
    suggestions.add("//...")
    // All rule targets in the top-level package, if there is a `BUILD` file at the root of the workspace.
    suggestions.add("//:all")
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
    isProjectRootDependentFormat = prefix[0] == '/'
    prefixSegments += prefix
      .removePrefix("/").removePrefix("/").split(":")[0]
      .split("/").size
  }

  // Add all subpaths to use with wildcards
  availableSuggestions
    .forEach { s ->
      patterns.add(s)
      val suggestionSegments = s.removePrefix("//").split(":")[0].split("/")
      suggestionSegments.indices.reversed()
        .filter { it + 1 >= prefixSegments }
        .forEach { i ->
          availablePathsSet.add(suggestionSegments.subList(0, i + 1).joinToString("/"))
        }
    }

  if (isProjectRootDependentFormat) {
    availablePathsSet.forEach { path ->
      patterns.add("//$path:all")
      patterns.add("//$path/...:all-targets")
      patterns.add("//$path/...:all")
    }
  }
  else {
    availablePathsSet.forEach { path ->
      patterns.add("$path:all")
      patterns.add("$path/...:all")
    }
  }

  return patterns
}
