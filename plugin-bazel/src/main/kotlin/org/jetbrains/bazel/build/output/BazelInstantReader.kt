package org.jetbrains.bazel.build.output

import com.intellij.build.output.BuildOutputInstantReader
import java.util.ArrayDeque

/**
 * A BuildOutputInstantReader backed by a shared deque of lines. It allows parsers to
 * read subsequent lines after the head line passed to BuildOutputParser.parse.
 */
class BazelInstantReader(
  private val lines: ArrayDeque<String>,
  private val parentIdProvider: () -> Any
) : BuildOutputInstantReader {

  // index starts at 1 because the current head line is provided separately to parser.parse()
  private var index: Int = 1

  override fun getParentEventId(): Any = parentIdProvider()

  override fun readLine(): String? {
    val size = lines.size
    return if (index < size) {
      val value = lines.elementAt(index)
      index++
      value
    } else null
  }

  override fun pushBack() {
    if (index > 1) index--
  }

  override fun pushBack(numberOfLines: Int) {
    index = (index - numberOfLines).coerceAtLeast(1)
  }

  /** Number of lines consumed beyond the head (0..n). */
  val consumedBeyondHead: Int get() = (index - 1).coerceAtLeast(0)
}
