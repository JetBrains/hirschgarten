package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.*
import com.google.devtools.build.lib.server.FailureDetails
import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@TestApplication
class ActionCompletedParserTest {

  @TempDir
  lateinit var tempDir: Path

  private val parser = ActionCompletedParser()

  @Test
  fun `returns null for successful action`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(true)
      )
      .build()

    val result = parser.parse(event)

    assertNull(result, "Successful actions should not generate events")
  }

  @Test
  fun `parses failed action with stderr file`() {
    val stderrFile = tempDir.resolve("stderr.txt")
    stderrFile.writeText("error: something went wrong\nat file.cpp:42")

    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
              .setPrimaryOutput("pkg/target.o")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setStderr(
            File.newBuilder()
              .setUri(stderrFile.toUri().toString())
              .setName("stderr.txt")
          )
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Action failed")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    assertEquals(MessageEvent.Kind.ERROR, (result as MessageEvent).kind)
    assertTrue(result.message.contains("//pkg:target"))
    assertTrue(result.description!!.contains("something went wrong"))
  }

  @Test
  fun `parses action with failure detail but no stderr`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Build failed: timeout exceeded")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    assertEquals(MessageEvent.Kind.ERROR, (result as MessageEvent).kind)
    assertTrue(result.description!!.contains("timeout exceeded"))
  }

  @Test
  fun `parses action with warning (no failure detail)`() {
    val stderrFile = tempDir.resolve("warning.txt")
    stderrFile.writeText("warning: deprecated API usage")

    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(true)
          .setStderr(
            File.newBuilder()
              .setUri(stderrFile.toUri().toString())
              .setName("warning.txt")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    assertEquals(MessageEvent.Kind.WARNING, (result as MessageEvent).kind)
    assertTrue(result.message.contains("BUILD_WARNING"))
    assertTrue(result.description!!.contains("deprecated API"))
  }

  @Test
  fun `skips external project labels`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("@external//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("External build failed")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNull(result, "External project issues should be skipped")
  }

  @Test
  fun `handles invalid stderr URI gracefully`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setStderr(
            File.newBuilder()
              .setUri("not a valid uri ://bad")
              .setName("stderr.txt")
          )
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Action failed")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    assertTrue((result as MessageEvent).description!!.contains("Invalid output URI"))
  }

  @Test
  fun `handles missing stderr file gracefully`() {
    val nonexistentFile = tempDir.resolve("nonexistent.txt")

    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setStderr(
            File.newBuilder()
              .setUri(nonexistentFile.toUri().toString())
              .setName("nonexistent.txt")
          )
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Action failed")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    assertTrue((result as MessageEvent).description!!.contains("Could not read output file"))
  }

  @Test
  fun `returns null for action without ActionExecuted payload`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      // No .setAction()
      .build()

    val result = parser.parse(event)

    assertNull(result)
  }

  @Test
  fun `returns null for non-ActionCompleted event`() {
    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setTestResult(BuildEventId.TestResultId.newBuilder().setLabel("//pkg:test"))
      )
      .setTestResult(TestResult.getDefaultInstance())
      .build()

    val result = parser.parse(event)

    assertNull(result)
  }

  @Test
  fun `handles invalid label format gracefully`() {
    val stderrFile = tempDir.resolve("stderr.txt")
    stderrFile.writeText("error message")

    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("invalid:::label:::format")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setStderr(
            File.newBuilder()
              .setUri(stderrFile.toUri().toString())
              .setName("stderr.txt")
          )
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Action failed")
          )
      )
      .build()

    val result = parser.parse(event)

    // Invalid/external labels should not generate events
    assertNull(result)
  }

  @Test
  fun `handles compiler error output format`() {
    val stderrFile = tempDir.resolve("stderr.txt")
    stderrFile.writeText("""
      src/main.cpp:42:10: error: use of undeclared identifier 'foo'
        return foo + 1;
               ^
      1 error generated.
    """.trimIndent())

    val event = BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel("//src:main")
          )
      )
      .setAction(
        ActionExecuted.newBuilder()
          .setSuccess(false)
          .setStderr(
            File.newBuilder()
              .setUri(stderrFile.toUri().toString())
          )
          .setFailureDetail(
            FailureDetails.FailureDetail.newBuilder()
              .setMessage("Compilation failed")
          )
      )
      .build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("use of undeclared identifier"))
    assertTrue(msg.description!!.contains("src/main.cpp:42:10"))
  }
}
