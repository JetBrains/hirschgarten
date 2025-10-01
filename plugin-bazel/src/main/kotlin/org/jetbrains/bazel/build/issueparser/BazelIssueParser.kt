package org.jetbrains.bazel.build.issueparser

import com.google.common.base.Ascii
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Parses Bazel build output for compile errors, warnings, and other issues.
 *
 * Ported from Google's Bazel plugin (BlazeIssueParser).
 */
class BazelIssueParser(private val parsers: List<Parser>) {
  private val previousLines = mutableListOf<String>()

  fun parseIssue(line: String): IssueOutput? {
    for (parser in parsers) {
      val result = parser.parse(line, previousLines)
      when {
        result.needsMoreInput -> {
          previousLines.add(line)
          return null
        }
        result.output != null -> {
          previousLines.clear()
          return result.output
        }
        else -> continue
      }
    }
    previousLines.clear()
    return null
  }

  /** Result from parsing the current line */
  data class ParseResult(
    val needsMoreInput: Boolean,
    val output: IssueOutput?
  ) {
    companion object {
      val NEEDS_MORE_INPUT = ParseResult(true, null)
      val NO_RESULT = ParseResult(false, null)

      fun output(output: IssueOutput): ParseResult = ParseResult(false, output)
    }
  }

  /** Parser interface - can consume single or multiple lines */
  interface Parser {
    fun parse(currentLine: String, previousLines: List<String>): ParseResult
  }

  /** Base class for parsers that match a single line via regex */
  abstract class SingleLineParser(regex: String) : Parser {
    protected val pattern: Pattern = Pattern.compile(regex)

    override fun parse(currentLine: String, previousLines: List<String>): ParseResult {
      check(previousLines.isEmpty()) { "SingleLineParser received multiple lines of input" }
      return parse(currentLine)
    }

    private fun parse(line: String): ParseResult {
      val matcher = pattern.matcher(line)
      if (matcher.find()) {
        val issue = createIssue(matcher)
        return if (issue != null) ParseResult.output(issue) else ParseResult.NO_RESULT
      }
      return ParseResult.NO_RESULT
    }

    protected abstract fun createIssue(matcher: Matcher): IssueOutput?
  }

  companion object {
    private const val BAZEL_BUILD_FILES_PATTERN = "(/.*?BUILD(?:\\.bazel)?)"

    fun fileFromAbsolutePath(absolutePath: String): File = File(absolutePath)

    /** Falls back to returning -1 if no integer can be parsed */
    fun parseOptionalInt(intString: String?): Int {
      if (intString == null) return -1
      return intString.toIntOrNull() ?: -1
    }

    /** Converts message type string to MessageEvent.Kind */
    fun messageKind(messageType: String?): MessageEvent.Kind {
      if (messageType == null) return MessageEvent.Kind.ERROR
      return when (Ascii.toLowerCase(messageType)) {
        "warning" -> MessageEvent.Kind.WARNING
        "note", "message", "context", "info" -> MessageEvent.Kind.INFO
        "error", "fatal error", "internal problem" -> MessageEvent.Kind.ERROR
        else -> MessageEvent.Kind.ERROR
      }
    }
  }
}

/** Represents a parsed issue with location and message information */
data class IssueOutput(
  val message: String,
  val kind: MessageEvent.Kind,
  val file: File? = null,
  val line: Int = -1,
  val column: Int = -1
) {
  fun toBuildEvent(parentId: Any): BuildEvent {
    val builder = BuildEvents.getInstance().fileMessage()
      .withParentId(parentId)
      .withKind(kind)
      .withMessage(message)

    if (file != null) {
      val filePosition = com.intellij.build.FilePosition(
        file,
        if (line >= 0) line - 1 else 0, // Convert to 0-based
        if (column >= 0) column - 1 else 0
      )
      builder.withFilePosition(filePosition)
    }

    return builder.build()
  }

  companion object {
    fun error(message: String) = Builder(message, MessageEvent.Kind.ERROR)
    fun warning(message: String) = Builder(message, MessageEvent.Kind.WARNING)
    fun info(message: String) = Builder(message, MessageEvent.Kind.INFO)
    fun issue(kind: MessageEvent.Kind, message: String) = Builder(message, kind)
  }

  class Builder(private val message: String, private val kind: MessageEvent.Kind) {
    private var file: File? = null
    private var line: Int = -1
    private var column: Int = -1

    fun withFile(file: File?, line: Int = -1, column: Int = -1): Builder {
      this.file = file
      this.line = line
      this.column = column
      return this
    }

    fun build(): IssueOutput = IssueOutput(message, kind, file, line, column)
  }
}
