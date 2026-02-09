package org.jetbrains.bazel.server.bep

import org.jetbrains.bazel.label.Label
import org.jetbrains.kotlin.konan.file.File
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

class RemoteExecutionPathRemapper(
  val lastActionCompletedFailedLabel: Label,
  val sourcePaths: List<String>,
  val workspaceRoot: Path
) {

  /**
   * Processes a raw multi-line string (e.g. full compiler output).
   * For each line that contains a /tmp/worker/cache/..._dir/ path,
   * attempts to remap the file path to a known source path.
   * If remapping succeeds, the line is rewritten with the source path.
   * Non-matching lines are passed through unchanged.
   */
  fun processOutput(rawOutput: String): String? {
    val lines = rawOutput.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val rewrittenOutput = StringBuilder()

    var matchFound = false
    for (line in lines) {
      val trimmed = line.trim()
      val errorMatcher: Matcher = ERROR_LINE_PATTERN.matcher(trimmed)

      if (!errorMatcher.matches()) {
        rewrittenOutput.append(line).append(System.lineSeparator())
        continue
      }
      matchFound = true

      val filePath = errorMatcher.group(1)
      val rest = errorMatcher.group(2)

      val cacheMatcher: Matcher = CACHE_PREFIX_PATTERN.matcher(filePath)

      if (!cacheMatcher.matches()) {
        rewrittenOutput.append(line).append(System.lineSeparator())
        continue
      }

      val relativePath = cacheMatcher.group(2)
      val matchedSources = findAllMatches( relativePath)

      if (matchedSources.isEmpty()) {
        rewrittenOutput.append(line).append(System.lineSeparator())
      } else {
        val sourcePath = matchedSources.first()
        rewrittenOutput
          .append(workspaceRoot)
          .append(File.separator)
          .append("$sourcePath:$rest")
          .append(System.lineSeparator())
      }
    }

    return if (matchFound) {
      rewrittenOutput.toString()
    } else {
      null
    }
  }

  /**
   * Finds all source paths that share the longest common tail of path
   * segments with the given error path. If multiple sources tie for the
   * best score, all of them are returned.
   */
  private fun findAllMatches(errorFilePath: String): List<String> {
    val errorSegments: Array<String?> = errorFilePath.replace('\\', '/').split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    val bestMatches = mutableListOf<String>()
    var bestScore = 0

    for (sourcePath in sourcePaths) {
      val sourceSegments: Array<String?> = sourcePath.replace('\\', '/').split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      val commonTail = countCommonTailSegments(sourceSegments, errorSegments)

      if (commonTail > 0 && commonTail > bestScore) {
        bestScore = commonTail
        bestMatches.clear()
        bestMatches.add(sourcePath)
      } else if (commonTail > 0 && commonTail == bestScore) {
        bestMatches.add(sourcePath)
      }
    }

    return bestMatches
  }

  /**
   * Counts how many path segments match when comparing two paths
   * from the end (reverse order).
   */
  private fun countCommonTailSegments(a: Array<String?>, b: Array<String?>): Int {
    var count = 0
    var ai = a.size - 1
    var bi = b.size - 1

    while (ai >= 0 && bi >= 0) {
      if (a[ai] == b[bi]) {
        count++
        ai--
        bi--
      } else {
        break
      }
    }
    return count
  }

  companion object {
    // Matches lines starting with a .java file path followed by :<rest>
    private val ERROR_LINE_PATTERN: Pattern = Pattern.compile("^(.+\\.java):(.+)$")

    // Matches the cache prefix: /tmp/worker/cache/<hex>_dir/...
    private val CACHE_PREFIX_PATTERN: Pattern = Pattern.compile("^(/tmp/worker/cache/[0-9a-z]+_dir/)(.+)$")
  }

}
