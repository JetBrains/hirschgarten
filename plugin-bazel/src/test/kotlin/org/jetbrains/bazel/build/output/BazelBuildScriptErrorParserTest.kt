package org.jetbrains.bazel.build.output

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.output.BuildOutputInstantReader
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.ArrayDeque
import java.util.function.Consumer

@org.junit.jupiter.api.extension.ExtendWith(com.intellij.testFramework.junit5.impl.TestApplicationExtension::class)
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
}
