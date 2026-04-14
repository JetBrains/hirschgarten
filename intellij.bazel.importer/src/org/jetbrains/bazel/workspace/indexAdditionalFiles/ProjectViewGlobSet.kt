package org.jetbrains.bazel.workspace.indexAdditionalFiles

import com.intellij.openapi.fileTypes.FileNameMatcher
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.jps.model.fileTypes.FileNameMatcherFactory
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeToOrNull

/**
 * A list of glob patterns, matching either the filename or the workspace-relative path.
 * TODO: revise after project view refactor PR is merged
 */
@ApiStatus.Internal
class ProjectViewGlobSet {
  private val rootDir: Path
  private val acceptedFilenames = mutableSetOf<String>()
  private val acceptedExtensions = mutableSetOf<String>()
  private val matchers = mutableListOf<FileNameMatcher>()

  constructor(rootDir: Path, patterns: List<String>) {
    require(rootDir.isAbsolute)
    this.rootDir = rootDir
    for (pattern in patterns) {
      addPattern(pattern)
    }
  }

  /**
   * @param path relative path from [rootDir] or an absolute path
   */
  fun matches(path: Path): Boolean {
    val relativeFromWorkspaceRoot = if (path.isAbsolute) {
      path.relativeToOrNull(rootDir) ?: return false
    }
    else {
      path
    }
    return matches(relativeFromWorkspaceRoot.invariantSeparatorsPathString)
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
   * @param path path from workspace root. The path must use forward slashes /
   */
  private fun matches(path: String): Boolean {
    val filename = path.substringAfterLast("/")
    if (filename in acceptedFilenames) return true
    if (filename.substringAfterLast('.', "") in acceptedExtensions) return true
    return matchers.any { it.acceptsCharSequence(path) }
  }
}
