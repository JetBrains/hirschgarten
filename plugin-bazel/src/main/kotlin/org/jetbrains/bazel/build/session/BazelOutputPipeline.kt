package org.jetbrains.bazel.build.session

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputParser
import com.intellij.execution.process.AnsiEscapeDecoder
import org.jetbrains.bazel.build.BazelOutputParserProvider
import org.jetbrains.bazel.build.output.BazelInstantReader
import java.util.ArrayDeque
import java.util.function.Consumer

/**
 * Drives the list of BuildOutputParsers against the Bazel process output.
 * Maintains a shared deque so parsers can drain subsequent lines via BuildOutputInstantReader.
 */
import org.jetbrains.annotations.TestOnly

class BazelOutputPipeline(private val session: BazelBuildSession, private val parsers: List<BuildOutputParser>) {

  private val lines: ArrayDeque<String> = ArrayDeque()
  private val deduplicator = BazelBuildEventDeduplicator()
  private val ansiDecoder = AnsiEscapeDecoder()

  fun onLine(text: String) {
    val normalized = text.removeSuffix("\n").removeSuffix("\r")
    if (normalized.isEmpty()) return

    // Decode ANSI sequences before parsing so stock parsers can recognize patterns
    val decoded = StringBuilder()
    ansiDecoder.escapeText(normalized, com.intellij.execution.process.ProcessOutputType.STDOUT) { decodedText, _ ->
      decoded.append(decodedText)
    }

    // Enqueue the new decoded line and try to parse from the head as long as we make progress.
    lines.addLast(decoded.toString())

    val dedup = this.deduplicator
    val consumer = Consumer<BuildEvent> { event ->
      if (dedup.shouldAccept(event)) {
        session.accept(event)
      }
    }
    processQueue@ while (lines.isNotEmpty()) {
      val head = lines.first()
      val reader = BazelInstantReader(lines) { session.currentParentId() }

      for (parser in parsers) {
        val consumed = try {
          parser.parse(head, reader, consumer)
        } catch (t: Throwable) {
          // Never let a faulty parser break the stream; skip this parser and try the next one for the same head line
          continue
        }

        if (consumed) {
          // Drop head + however many additional lines the parser consumed.
          val toDrop = 1 + reader.consumedBeyondHead
          repeat(toDrop.coerceAtMost(lines.size)) { lines.removeFirst() }
          continue@processQueue
        }
      }

      // No parser consumed the head yet. Keep it in the deque to allow multi-line parsers
      // (e.g., Javac/Kotlinc) to consume it once more lines arrive. To prevent unbounded growth,
      // drop the oldest line only when the buffer exceeds the safety limit.
      if (lines.size > LOOKAHEAD_BUFFER_LIMIT) {
        lines.removeFirst()
      } else {
        break@processQueue
      }
    }
  }

  private companion object {
    // Upper bound on buffered lines to avoid unbounded memory usage when no parser matches.
    private const val LOOKAHEAD_BUFFER_LIMIT: Int = 256
  }
}
