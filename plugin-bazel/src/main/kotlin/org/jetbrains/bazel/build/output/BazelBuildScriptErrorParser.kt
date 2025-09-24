package org.jetbrains.bazel.build.output

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.openapi.util.NlsSafe
import java.io.File
import java.util.function.Consumer

/**
 * Parses Bazel loading-phase errors that point to BUILD/WORKSPACE/.bzl files and reports them
 * directly to the Build View as navigable FileMessageEvent.
 *
 * Typical formats include:
 *  - ERROR: path/to/BUILD:12:34: message
 *  - ERROR: path/to/file.bzl:27: message
 *  - ERROR: path/to/WORKSPACE:5:1: message
 */
class BazelBuildScriptErrorParser : BuildOutputParser {

  private val headerRegex = Regex(
    pattern = "^ERROR: (.+?):(\\d+)(?::(\\d+))?: (.+)$"
  )

  override fun parse(
    line: @NlsSafe String,
    reader: BuildOutputInstantReader,
    messageConsumer: Consumer<in BuildEvent>
  ): Boolean {
    val match = headerRegex.matchEntire(line.trim()) ?: return false

    val filePath = match.groupValues[1]
    val lineNo = match.groupValues[2].toIntOrNull()?.let { (it - 1).coerceAtLeast(0) } ?: 0
    val colNo = match.groupValues.getOrNull(3)?.toIntOrNull()?.let { (it - 1).coerceAtLeast(0) } ?: 0
    val rawMessage = match.groupValues[4].trim()

    val file = File(filePath)
    val position = FilePosition(file, lineNo, colNo)

    // Keep the first line short; attach the full line as description
    val title = rawMessage
    val description = line

    val event = BuildEvents.getInstance().fileMessage()
      .withParentId(reader.parentEventId)
      .withKind(MessageEvent.Kind.ERROR)
      .withMessage(title)
      .withDescription(description)
      .withFilePosition(position)
      .build()
    messageConsumer.accept(event)
    return true
  }
}
