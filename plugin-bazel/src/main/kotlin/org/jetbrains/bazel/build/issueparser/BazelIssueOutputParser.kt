package org.jetbrains.bazel.build.issueparser

import com.intellij.build.events.BuildEvent
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import java.io.File
import java.util.function.Consumer

/**
 * Adapter that bridges BazelIssueParser (ported from Google plugin) with IntelliJ's BuildOutputParser.
 *
 * This allows the old plugin's sophisticated issue parsing logic to work with IntelliJ's
 * build output pipeline.
 */
class BazelIssueOutputParser(
  private val project: Project,
  private val workspaceRoot: File
) : BuildOutputParser {

  private val issueParser = BazelIssueParser(createDefaultParsers())

  override fun parse(
    line: @NlsSafe String,
    reader: BuildOutputInstantReader,
    messageConsumer: Consumer<in BuildEvent>
  ): Boolean {
    val issue = issueParser.parseIssue(line)
    if (issue != null) {
      val event = issue.toBuildEvent(reader.parentEventId)
      messageConsumer.accept(event)
      return true
    }
    return false
  }

  private fun createDefaultParsers(): List<BazelIssueParser.Parser> {
    // Only include Bazel-specific parsers here.
    // Generic compiler errors (Java, Kotlin, C++, etc.) are handled by IntelliJ's built-in parsers
    // (JavacOutputParser, KotlincOutputParser) which run before this parser in the chain.
    return listOf(
      // Bazel-specific: BUILD/Starlark files
      BuildParser(),
      StarlarkErrorParser(),
      LinelessBuildParser(),
      FileNotFoundBuildParser(workspaceRoot),
      // Java/Javac errors (fallback if IntelliJ's JavacOutputParser misses them)
      BazelJavacErrorParser(project, workspaceRoot),
      // Kotlin/Kotlinc errors (fallback if IntelliJ's KotlincOutputParser misses them)
      BazelKotlincErrorParser(project, workspaceRoot),
      // Python-specific (not well handled by built-in parsers)
      PythonCompileParser(project),
      TracebackParser(),
      // Generic ERROR: catchall (lowest priority)
      GenericErrorParser()
    )
  }
}
