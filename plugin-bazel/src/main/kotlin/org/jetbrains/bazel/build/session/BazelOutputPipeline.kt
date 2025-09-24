package org.jetbrains.bazel.build.session

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputParser
import org.jetbrains.bazel.build.BazelOutputParserProvider
import org.jetbrains.bazel.build.output.BazelInstantReader
import java.util.ArrayDeque
import java.util.function.Consumer

/**
 * Drives the list of BuildOutputParsers against the Bazel process output.
 * Maintains a shared deque so parsers can drain subsequent lines via BuildOutputInstantReader.
 */
import org.jetbrains.annotations.TestOnly

class BazelOutputPipeline(private val session: BazelBuildSession) {

  private lateinit var parsers: List<BuildOutputParser>
  private val lines: ArrayDeque<String> = ArrayDeque()
  private val deduplicator = BazelBuildEventDeduplicator()
  private val ansiDecoder = com.intellij.execution.process.AnsiEscapeDecoder()

  @TestOnly
  internal constructor(session: BazelBuildSession, parsers: List<BuildOutputParser>) : this(session) {
    this.parsers = parsers
  }

  init {
    if (!this::parsers.isInitialized) {
      parsers = BazelOutputParserProvider().getBuildOutputParsersForStandalone()
    }
  }

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

      // No parser consumed the head; do not emit an OutputBuildEvent to avoid duplicating console output.
      // The attached process console already renders this line (with colors if applicable).
      lines.removeFirst()
    }
  }
}
