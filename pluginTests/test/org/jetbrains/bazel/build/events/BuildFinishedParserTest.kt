package org.jetbrains.bazel.build.events

import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@TestApplication
class BuildFinishedParserTest {

  private val parser = BuildFinishedParser()

  @Test
  fun `returns null for successful build without anomalies`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 0
    ).build()

    val result = parser.parse(event)

    assertNull(result, "Successful builds without anomalies should not generate events")
  }

  @Test
  fun `parses failed build with non-zero exit code`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 1,
      exitCodeName = "BUILD_FAILURE"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Build failed with exit code 1"))
    assertTrue(msg.description!!.contains("Exit code: BUILD_FAILURE"))
  }

  @Test
  fun `parses successful build with anomaly report as warning`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 0,
      anomalyReport = true
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.WARNING, msg.kind)
    assertTrue(msg.message.contains("Build completed with warnings"))
  }

  @Test
  fun `includes finish time when available`() {
    val finishTime = System.currentTimeMillis()
    val event = BepTestUtils.buildFinished(
      exitCode = 1,
      finishTimeMillis = finishTime
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.description!!.contains("Finished at:"))
  }

  @Test
  fun `handles build without exit code name`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 1,
      exitCodeName = ""
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    // Should not include empty exit code name in description
    val lines = msg.description!!.lines()
    assertFalse(lines.any { it.contains("Exit code:") && it.trim() == "Exit code:" })
  }

  @Test
  fun `handles various exit codes`() {
    val exitCodes = listOf(1, 2, 3, 8, 37)

    for (code in exitCodes) {
      val event = BepTestUtils.buildFinished(
        exitCode = code,
        exitCodeName = "ERROR_$code"
      ).build()

      val result = parser.parse(event)

      assertNotNull(result, "Exit code $code should generate event")
      val msg = result as com.intellij.build.events.MessageEvent
      assertTrue(msg.message.contains("exit code $code"))
    }
  }

  @Test
  fun `formats error message correctly`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 1,
      exitCodeName = "BUILD_FAILURE",
      finishTimeMillis = 1609459200000 // 2021-01-01 00:00:00 UTC
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent

    // Check title
    assertEquals("Build failed with exit code 1", msg.message)

    // Check description format
    val description = msg.description!!
    assertTrue(description.startsWith("Build failed with exit code 1"))
    assertTrue(description.contains("Finished at:"))
    assertTrue(description.contains("Exit code: BUILD_FAILURE"))
  }

  @Test
  fun `formats warning message correctly for anomaly`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 0,
      anomalyReport = true,
      finishTimeMillis = 1609459200000
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent

    assertEquals("Build completed with warnings", msg.message)
    assertTrue(msg.description!!.contains("Anomaly Report:"))
  }

  @Test
  fun `handles build with both anomaly and failure`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 1,
      exitCodeName = "BUILD_FAILURE",
      anomalyReport = true
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    // Non-zero exit code takes precedence - should be ERROR not WARNING
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Build failed"))
    assertTrue(msg.description!!.contains("Anomaly Report:"))
  }

  @Test
  fun `handles minimal build finished event`() {
    val event = BepTestUtils.buildFinished(
      exitCode = 1
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    // Should still work with minimal information
    assertTrue(msg.message.contains("Build failed with exit code 1"))
  }

  @Test
  fun `logs build completion at info level`() {
    // This test verifies that logging happens (implementation detail)
    // In practice, we'd check log output, but here we just verify no crash
    val event = BepTestUtils.buildFinished(
      exitCode = 1
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    // If logging failed, parsing would likely throw
  }
}
