package org.jetbrains.bazel.build.issueparser

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.build.issueparser.BazelIssueParser.Companion.fileFromAbsolutePath
import org.jetbrains.bazel.build.issueparser.BazelIssueParser.Companion.messageKind
import org.jetbrains.bazel.build.issueparser.BazelIssueParser.Companion.parseOptionalInt
import java.io.File
import java.util.regex.Matcher

/**
 * Individual parser implementations for different types of Bazel build issues.
 *
 * Ported from Google's Bazel plugin issue parsers.
 */

private const val BAZEL_BUILD_FILES_PATTERN = "(/.*?BUILD(?:\\.bazel)?)"

/**
 * Parses Python compilation errors.
 * Format: File "path.py", line 123, message
 */
class PythonCompileParser(private val project: Project) :
  BazelIssueParser.SingleLineParser(
    "^File \"([^:]*\\.py)\", " + // file path
      "line ([0-9]+), " + // line number
      "(.*)$" // message
  ) {

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val file = FileResolver.resolveToFile(project, matcher.group(1))
    return IssueOutput.error(matcher.group(3))
      .withFile(file, matcher.group(2).toInt(), 0)
      .build()
  }
}

/**
 * Parses generic compiler output errors.
 * Format: file.ext:123:45: error: message
 *
 * This is kept for reference but should NOT be used - it conflicts with IntelliJ's built-in parsers.
 */
@Deprecated("Use IntelliJ's built-in JavacOutputParser and KotlincOutputParser instead")
class DefaultCompileParser(private val project: Project) :
  BazelIssueParser.SingleLineParser(
    "^" + // start
      "([^:]+)" + // file path
      ":([0-9]+)" + // line number
      "(?::([0-9]+))?" + // optional column number
      "(?::| -) " + // colon or hyphen separator
      "(?i:" + // optional case insensitive message type
      "(fatal error|error|warning|note|internal problem|context|info)" +
      "(?::| -)? " + // optional colon or hyphen separator
      ")?" +
      "(.*)$" // message
  ) {

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val file = FileResolver.resolveToFile(project, matcher.group(1))
    val kind = messageKind(matcher.group(4))
    return IssueOutput.issue(kind, matcher.group(5))
      .withFile(file, matcher.group(2).toInt(), parseOptionalInt(matcher.group(3)))
      .build()
  }
}

/**
 * Parses Java/Javac errors that IntelliJ's JavacOutputParser might miss.
 * Specifically handles relative paths in Bazel output.
 * Format: path/to/File.java:123: error: message
 * Format: path/to/File.java:123:45: error: message
 */
class BazelJavacErrorParser(private val project: Project, private val workspaceRoot: File) :
  BazelIssueParser.SingleLineParser(
    "^" + // start
      "([^:]+\\.java)" + // Java file path (must end with .java)
      ":([0-9]+)" + // line number
      "(?::([0-9]+))?" + // optional column number
      ": " + // colon separator
      "(?:(error|warning))?" + // optional error/warning keyword
      ":? " + // optional colon
      "(.*)$" // message
  ) {

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val relativePath = matcher.group(1)
    val file = resolveJavaFile(relativePath)

    // Determine severity
    val severityKeyword = matcher.group(4)
    val kind = when (severityKeyword?.lowercase()) {
      "warning" -> MessageEvent.Kind.WARNING
      else -> MessageEvent.Kind.ERROR
    }

    val message = matcher.group(5)
    return IssueOutput.issue(kind, message)
      .withFile(file, matcher.group(2).toInt(), parseOptionalInt(matcher.group(3)))
      .build()
  }

  private fun resolveJavaFile(relativePath: String): File? {
    // Try relative to workspace root
    val fromRoot = File(workspaceRoot, relativePath)
    if (fromRoot.exists()) return fromRoot

    // Try relative to project base path
    val basePath = project.basePath
    if (basePath != null) {
      val fromBase = File(basePath, relativePath)
      if (fromBase.exists()) return fromBase
    }

    // Return the file anyway even if it doesn't exist - IntelliJ will still create a link
    return fromRoot
  }
}

/**
 * Parses Python traceback errors (multi-line).
 * Format:
 *   ERROR: file.py:123:45: Traceback (most recent call last):
 *     ... indented lines ...
 *   final error line
 */
class TracebackParser : BazelIssueParser.Parser {
  private val pattern = Regex(
    "(ERROR): (.*?):([0-9]+):([0-9]+): (Traceback \\(most recent call last\\):)"
  )

  override fun parse(
    currentLine: String,
    previousLines: List<String>
  ): BazelIssueParser.ParseResult {
    if (previousLines.isEmpty()) {
      return if (pattern.containsMatchIn(currentLine)) {
        BazelIssueParser.ParseResult.NEEDS_MORE_INPUT
      } else {
        BazelIssueParser.ParseResult.NO_RESULT
      }
    }

    if (currentLine.startsWith("\t")) {
      return BazelIssueParser.ParseResult.NEEDS_MORE_INPUT
    } else {
      val match = pattern.find(previousLines[0])
      check(match != null) { "Found a match in the first line previously, but now it isn't there" }

      val message = buildString {
        append(match.groupValues[5])
        for (i in 1 until previousLines.size) {
          append(System.lineSeparator())
          append(previousLines[i])
        }
        append(System.lineSeparator())
        append(currentLine)
      }

      return BazelIssueParser.ParseResult.output(
        IssueOutput.error(message)
          .withFile(
            File(match.groupValues[2]),
            match.groupValues[3].toInt(),
            parseOptionalInt(match.groupValues[4])
          )
          .build()
      )
    }
  }
}

/**
 * Parses BUILD file errors.
 * Format: ERROR: /path/to/BUILD:123:45: message
 */
class BuildParser : BazelIssueParser.SingleLineParser(
  "^ERROR: $BAZEL_BUILD_FILES_PATTERN:([0-9]+):([0-9]+): (.*)\$"
) {
  override fun createIssue(matcher: Matcher): IssueOutput? {
    val message = matcher.group(4)
    // Skip generic "Couldn't build file" messages unless they contain specific info
    if (message.startsWith("Couldn't build file ") &&
      !message.contains("Executing genrule")
    ) {
      return null
    }

    val file = fileFromAbsolutePath(matcher.group(1))
    return IssueOutput.error(message)
      .withFile(file, matcher.group(2).toInt(), parseOptionalInt(matcher.group(3)))
      .build()
  }
}

/**
 * Parses Starlark (.bzl) file errors.
 * Format: ERROR: /path/to/file.bzl:123:45: message
 */
class StarlarkErrorParser : BazelIssueParser.SingleLineParser(
  "^ERROR: (/.*?\\.bzl):([0-9]+):([0-9]+): (.*)\$"
) {
  override fun createIssue(matcher: Matcher): IssueOutput? {
    val file = fileFromAbsolutePath(matcher.group(1))
    return IssueOutput.error(matcher.group(4))
      .withFile(file, matcher.group(2).toInt(), parseOptionalInt(matcher.group(3)))
      .build()
  }
}

/**
 * Parses BUILD errors without line numbers.
 * Format: ERROR: file:char offsets 123--456: message
 */
class LinelessBuildParser : BazelIssueParser.SingleLineParser(
  "^ERROR: (.*?):char offsets [0-9]+--[0-9]+: (.*)\$"
) {
  override fun createIssue(matcher: Matcher): IssueOutput? {
    return IssueOutput.error(matcher.group(2))
      .withFile(File(matcher.group(1)))
      .build()
  }
}

/**
 * Parses "file not found" BUILD errors.
 * Format: ERROR: ... Unable to load file 'target': message
 */
class FileNotFoundBuildParser(private val workspaceRoot: File) :
  BazelIssueParser.SingleLineParser(
    "^ERROR: .*? Unable to load file '(.*?)': (.*)\$"
  ) {

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val file = fileFromTarget(workspaceRoot, matcher.group(1))
    return IssueOutput.error(matcher.group(2))
      .withFile(file)
      .build()
  }

  private fun fileFromTarget(workspaceRoot: File, targetString: String): File? {
    // Parse target like "//foo/bar:baz" and resolve to file
    if (!targetString.startsWith("//") || targetString.contains("@")) {
      return null // External or invalid target
    }

    val withoutSlashes = targetString.substring(2)
    val colonIndex = withoutSlashes.indexOf(':')
    if (colonIndex < 0) {
      return File(workspaceRoot, withoutSlashes)
    }

    val packagePath = withoutSlashes.substring(0, colonIndex)
    val targetName = withoutSlashes.substring(colonIndex + 1)
    return File(File(workspaceRoot, packagePath), targetName)
  }
}

/**
 * Generic error parser - catches common ERROR: patterns not handled by specific parsers.
 */
class GenericErrorParser : BazelIssueParser.SingleLineParser(
  "^(ERROR): (.*)\$"
) {
  // Patterns to explicitly ignore
  private val ignorePatterns = listOf(
    Regex("//.*?: Exit [0-9]+\\."),
    Regex(".*: Process exited with status [0-9]+\\."),
    Regex("build interrupted\\."),
    Regex("Couldn't start the build. Unable to run tests\\."),
  )

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val fullMessage = matcher.group(2)

    // Check if this matches any ignore pattern
    if (ignorePatterns.any { it.matches(fullMessage) }) {
      return null
    }

    return IssueOutput.error(fullMessage).build()
  }
}

/**
 * Parses Kotlin/Kotlinc errors that IntelliJ's KotlincOutputParser might miss.
 * Specifically handles relative paths in Bazel output.
 * Format: path/to/File.kt:123: error: message
 * Format: path/to/File.kt:123:45: error: message
 */
class BazelKotlincErrorParser(private val project: Project, private val workspaceRoot: File) :
  BazelIssueParser.SingleLineParser(
    "^" + // start
      "([^:]+\\.kt)" + // Kotlin file path (must end with .kt)
      ":([0-9]+)" + // line number
      "(?::([0-9]+))?" + // optional column number
      ": " + // colon separator
      "(?:(error|warning))?" + // optional error/warning keyword
      ":? " + // optional colon
      "(.*)$" // message
  ) {

  override fun createIssue(matcher: Matcher): IssueOutput? {
    val relativePath = matcher.group(1)
    val file = resolveKotlinFile(relativePath)

    // Determine severity
    val severityKeyword = matcher.group(4)
    val kind = when (severityKeyword?.lowercase()) {
      "warning" -> MessageEvent.Kind.WARNING
      else -> MessageEvent.Kind.ERROR
    }

    val message = matcher.group(5)
    return IssueOutput.issue(kind, message)
      .withFile(file, matcher.group(2).toInt(), parseOptionalInt(matcher.group(3)))
      .build()
  }

  private fun resolveKotlinFile(relativePath: String): File? {
    // Try relative to workspace root
    val fromRoot = File(workspaceRoot, relativePath)
    if (fromRoot.exists()) return fromRoot

    // Try relative to project base path
    val basePath = project.basePath
    if (basePath != null) {
      val fromBase = File(basePath, relativePath)
      if (fromBase.exists()) return fromBase
    }

    // Return the file anyway even if it doesn't exist - IntelliJ will still create a link
    return fromRoot
  }
}

/**
 * Helper to resolve file paths relative to the project.
 * Delegates to the extensible FileResolver system.
 */
object FileResolver {
  fun resolveToFile(project: Project, path: String): File? {
    val resolved = org.jetbrains.bazel.build.fileresolver.FileResolver.resolve(project, path)
    return resolved?.toFile()
  }
}
