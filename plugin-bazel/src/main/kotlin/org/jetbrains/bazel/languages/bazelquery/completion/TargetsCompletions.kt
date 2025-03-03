package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.openapi.project.ProjectManager
import org.jetbrains.bazel.target.targetUtils


private val targets = ProjectManager.getInstance()
  .openProjects.firstOrNull()
  ?.targetUtils?.allTargets()?.map {
    it.toString().removePrefix("@")
  }

fun generateTargetCompletions(prefix: String): List<String> {
  val suggestions = mutableListOf<String>()
  val allTargets =  targets?.toSet() ?: emptySet()

  suggestions.addAll(allTargets.filter { it.startsWith(prefix) })
  if (prefix.isNotEmpty() && prefix[0].isLetter()) {
    suggestions.addAll(
      allTargets
        .filter { it.startsWith("//$prefix") }
        .map { it.removePrefix("//") }
    )
  }
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
  }
  if (prefix.startsWith(".") || prefix.isEmpty()) {
    // All rule targets in all packages beneath the working directory.
    suggestions.add("...:all")
  }

  return suggestions.distinct()
}

private fun generateTargetPatterns(prefix: String, suggestions: List<String>): List<String> {
  val patterns = mutableListOf<String>()

  val availablePathsSet = mutableSetOf<String>()
  var prefixStartsWithBackslash = false
  var prefixSegments: List<String> = emptyList()

  if (prefix.isNotEmpty()){
    prefixStartsWithBackslash = prefix[0] == '/'
    val targetPrefix = prefix.removePrefix("/").removePrefix("/").split(":")[0]
    prefixSegments = targetPrefix.split("/")
  }
  suggestions.forEach { s ->
    val suggestionSegments = s.removePrefix("//").split(":")[0].split("/")
    suggestionSegments.indices.reversed()
      .filter { it + 1 >= prefixSegments.size }
      .forEach { i ->
        availablePathsSet.add(suggestionSegments.subList(0, i + 1).joinToString("/"))
      }
  }

  if (prefixStartsWithBackslash) {
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
