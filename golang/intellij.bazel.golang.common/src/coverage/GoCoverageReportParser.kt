package org.jetbrains.bazel.golang.coverage

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.useLines

@ApiStatus.Internal
object GoCoverageReportParser {
  private val blockRegex = Regex("""^(.+):(\d+)\.\d+,(\d+)\.\d+\s+\d+\s+(\d+)$""")
  private const val MODE_PREFIX = "mode: "
  private const val MAX_COVERAGE_BLOCK_LINES = 1_000_000L

  fun parse(
    file: Path,
    onMalformedLine: (String) -> Unit = {},
    consumer: (String, Int, Long) -> Unit,
  ): Boolean = file.isRegularFile() && file.useLines { lines -> parse(lines, onMalformedLine, consumer) }

  fun parse(
    lines: Sequence<String>,
    onMalformedLine: (String) -> Unit = {},
    consumer: (String, Int, Long) -> Unit,
  ): Boolean {
    val iterator = lines.iterator()
    if (!iterator.hasNext() || !iterator.next().isModeLine()) return false

    while (iterator.hasNext()) {
      ProgressManager.checkCanceled()
      val line = iterator.next()
      val match = blockRegex.matchEntire(line)
      if (match == null) {
        onMalformedLine(line)
        continue
      }
      val filePath = match.groupValues[1]
      val startLine = match.groupValues[2].toIntOrNull()
      val endLine = match.groupValues[3].toIntOrNull()
      val hits = match.groupValues[4].toLongOrNull()
      if (startLine == null || endLine == null || hits == null ||
          (endLine.toLong() - startLine + 1) !in 1L..MAX_COVERAGE_BLOCK_LINES) {
        onMalformedLine(line)
        continue
      }

      for (lineNumber in startLine..endLine) {
        ProgressManager.checkCanceled()
        consumer(filePath, lineNumber, hits)
      }
    }
    return true
  }

  private fun String.isModeLine(): Boolean = startsWith(MODE_PREFIX)
}
