package org.jetbrains.bazel.workspace.indexAdditionalFiles

import com.intellij.openapi.fileTypes.FileNameMatcher
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory

/**
 * A list of glob patterns, matching either the filename or the workspace-relative path.
 * TODO: revise after project view refactor PR is merged
 */
class ProjectViewGlobSet {
  private val acceptedFilenames = mutableSetOf<String>()
  private val acceptedExtensions = mutableSetOf<String>()
  private val matchers = mutableListOf<FileNameMatcher>()

  constructor(patterns: List<String>) {
    for (pattern in patterns) {
      addPattern(pattern)
    }
  }

  private fun addPattern(pattern: String) {
    if ("/" !in pattern) {
      if ("*" !in pattern && "?" !in pattern) {
        acceptedFilenames.add(pattern)
        return
      } else if (pattern.startsWith("*.")) {
        if (pattern.indexOfAny("*?".toCharArray(), startIndex = 2) == -1) {
          acceptedExtensions.add(pattern.substring(2))
          return
        }
      }
    }

    // See https://github.com/bazelbuild/intellij/blob/e7aa7f57260ac473cfa1b072b01400f81eed925d/base/src/com/google/idea/blaze/base/sync/projectview/SourceTestConfig.java#L44
    val withAsteriskAtEnd =
      pattern
        .trimEnd('*')
        .trimEnd('/')
        .plus('*')
    matchers.add(FileNameMatcherFactory.getInstance().createMatcher(withAsteriskAtEnd))
  }

  /**
   * [path] should use forward slashes /
   */
  fun matches(path: String): Boolean {
    if (path.substringAfterLast("/") in acceptedFilenames) return true
    if (path.substringAfterLast('.', "") in acceptedExtensions) return true
    return matchers.any { it.acceptsCharSequence(path) }
  }
}
