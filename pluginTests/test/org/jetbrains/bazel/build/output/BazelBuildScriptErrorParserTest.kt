package org.jetbrains.bazel.build.output

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(TestApplicationExtension::class)
class BazelBuildScriptErrorParserTest {

  private fun reader(parentId: Any = "PARENT"): BuildOutputInstantReader = object : BuildOutputInstantReader {
    override fun getParentEventId(): Any = parentId
    override fun readLine(): String? = null
    override fun pushBack() {}
    override fun pushBack(numberOfLines: Int) {}
  }

  @Test
  fun `parses BUILD error with line and column`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:12:34: no such target 'foo'"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed, "Parser should consume BUILD error line")
    assertEquals(1, events.size)
    val evt = events.single() as FileMessageEvent
    assertEquals(MessageEvent.Kind.ERROR, evt.kind)
    assertEquals("no such target 'foo'", evt.message)

    val pos: FilePosition = evt.filePosition
    assertEquals(File("/ws/pkg/BUILD"), pos.file)
    assertEquals(11, pos.startLine) // 0-based
    assertEquals(33, pos.startColumn) // 0-based
  }

  @Test
  fun `parses WORKSPACE error without column defaults to col 0`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/WORKSPACE:5: unknown repository rule"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    val pos = evt.filePosition
    assertEquals(4, pos.startLine)
    assertEquals(0, pos.startColumn)
  }

  @Test
  fun `non-error line is ignored`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "INFO: some info line"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertFalse(consumed)
    assertTrue(events.isEmpty())
  }

  @Test
  fun `parses bzl file error`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/tools/build_defs.bzl:42:12: Variable foo is read only"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    assertEquals(1, events.size)
    val evt = events.single() as FileMessageEvent
    assertEquals(MessageEvent.Kind.ERROR, evt.kind)
    assertEquals("Variable foo is read only", evt.message)
    assertEquals(File("/ws/tools/build_defs.bzl"), evt.filePosition.file)
    assertEquals(41, evt.filePosition.startLine)
    assertEquals(11, evt.filePosition.startColumn)
  }

  @Test
  fun `parses BUILD_bazel file error`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD.bazel:10:5: no such target"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(File("/ws/pkg/BUILD.bazel"), evt.filePosition.file)
    assertEquals(9, evt.filePosition.startLine)
  }

  @Test
  fun `parses error without column number`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:42: Target not found"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(41, evt.filePosition.startLine)
    assertEquals(0, evt.filePosition.startColumn)
  }

  @Test
  fun `parses error with colon in message`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:12:34: error: invalid syntax: missing ']'"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals("error: invalid syntax: missing ']'", evt.message)
  }

  @Test
  fun `parses error with path containing spaces`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/my folder/pkg/BUILD:12:34: syntax error"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(File("/ws/my folder/pkg/BUILD"), evt.filePosition.file)
  }

  @Test
  fun `parses long error message`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:12:34: " +
      "This is a very long error message that describes in detail what went wrong " +
      "and provides helpful suggestions for how to fix the issue"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertTrue(evt.message.contains("very long error message"))
    assertTrue(evt.message.contains("helpful suggestions"))
  }

  @Test
  fun `includes full line in description`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:12:34: test error message"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(line, evt.description)
  }

  @Test
  fun `handles line number 1`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:1:1: error at file start"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(0, evt.filePosition.startLine)
    assertEquals(0, evt.filePosition.startColumn)
  }

  @Test
  fun `handles high line and column numbers`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "ERROR: /ws/pkg/BUILD:9999:888: error far in file"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertTrue(consumed)
    val evt = events.single() as FileMessageEvent
    assertEquals(9998, evt.filePosition.startLine)
    assertEquals(887, evt.filePosition.startColumn)
  }

  @Test
  fun `ignores WARNING lines`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val line = "WARNING: /ws/pkg/BUILD:12:34: this is just a warning"
    val consumed = parser.parse(line, reader()) { events.add(it) }

    assertFalse(consumed)
    assertTrue(events.isEmpty())
  }

  @Test
  fun `ignores malformed ERROR lines`() {
    val parser = BazelBuildScriptErrorParser()
    val events = mutableListOf<BuildEvent>()

    val malformedLines = listOf(
      "ERROR: invalid format",
      "ERROR:",
      "ERROR /no/colon/after/path",
      "not an error line"
    )

    for (line in malformedLines) {
      val consumed = parser.parse(line, reader()) { events.add(it) }
      assertFalse(consumed, "Should not consume: $line")
    }

    assertTrue(events.isEmpty())
  }
}
