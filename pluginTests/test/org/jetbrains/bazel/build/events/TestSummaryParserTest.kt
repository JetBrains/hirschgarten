package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TestStatus
import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@TestApplication
class TestSummaryParserTest {

  private val parser = TestSummaryParser()

  @Test
  fun `returns null for passed test summary`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.PASSED
    ).build()

    val result = parser.parse(event)

    assertNull(result, "Passed test summaries should not generate events")
  }

  @Test
  fun `returns null for NO_STATUS test summary`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.NO_STATUS
    ).build()

    val result = parser.parse(event)

    assertNull(result)
  }

  @Test
  fun `parses failed test summary`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 3,
      failed = listOf("/tmp/test1.xml", "/tmp/test2.xml")
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test Summary FAILED"))
    assertTrue(msg.message.contains("//test:unit_test"))
    assertTrue(msg.description!!.contains("Total runs: 3"))
  }

  @Test
  fun `parses flaky test summary as warning`() {
    val event = BepTestUtils.testSummary(
      label = "//test:flaky_test",
      overallStatus = TestStatus.FLAKY,
      totalRunCount = 5,
      passed = listOf("/tmp/pass1.xml", "/tmp/pass2.xml"),
      failed = listOf("/tmp/fail1.xml")
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertEquals(MessageEvent.Kind.WARNING, msg.kind)
    assertTrue(msg.message.contains("Test Summary FLAKY"))
  }

  @Test
  fun `parses timeout test summary`() {
    val event = BepTestUtils.testSummary(
      label = "//test:slow_test",
      overallStatus = TestStatus.TIMEOUT,
      totalRunCount = 1
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test Summary TIMEOUT"))
  }

  @Test
  fun `parses incomplete test summary`() {
    val event = BepTestUtils.testSummary(
      label = "//test:incomplete_test",
      overallStatus = TestStatus.INCOMPLETE,
      totalRunCount = 2
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test Summary INCOMPLETE"))
  }

  @Test
  fun `includes first start time when available`() {
    val startTime = System.currentTimeMillis()
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 1,
      firstStartTimeMillis = startTime
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("First start:"))
  }

  @Test
  fun `includes total duration when available`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 1,
      totalDurationSec = 42
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("Total duration: 42s"))
  }

  @Test
  fun `shows run attempts for multiple runs`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 5,
      passed = listOf("/tmp/pass1.xml", "/tmp/pass2.xml"),
      failed = listOf("/tmp/fail1.xml", "/tmp/fail2.xml", "/tmp/fail3.xml")
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("Run attempts:"))
    assertTrue(msg.description!!.contains("Passed:"))
    assertTrue(msg.description!!.contains("Failed:"))
  }

  @Test
  fun `handles single run without showing attempts`() {
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 1,
      failed = listOf("/tmp/fail.xml")
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    // Should not show "Run attempts:" for single run
    assertFalse(msg.description!!.contains("Run attempts:"))
  }

  @Test
  fun `handles summary with all timing information`() {
    val startTime = System.currentTimeMillis()
    val event = BepTestUtils.testSummary(
      label = "//test:unit_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 3,
      firstStartTimeMillis = startTime,
      totalDurationSec = 120
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("First start:"))
    assertTrue(msg.description!!.contains("Total duration: 120s"))
  }

  @Test
  fun `handles invalid label format gracefully`() {
    val event = BepTestUtils.testSummary(
      label = "invalid:::label",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 1
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    // Should still create event despite invalid label
    val msg = result as MessageEvent
    assertTrue(msg.message.contains("invalid:::label"))
  }

  @Test
  fun `formats complete summary correctly`() {
    val event = BepTestUtils.testSummary(
      label = "//test:comprehensive_test",
      overallStatus = TestStatus.FAILED,
      totalRunCount = 10,
      passed = listOf("/tmp/pass1.xml", "/tmp/pass2.xml", "/tmp/pass3.xml"),
      failed = listOf("/tmp/fail1.xml", "/tmp/fail2.xml"),
      firstStartTimeMillis = 1609459200000,
      totalDurationSec = 300
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent

    // Verify all expected sections are present
    val description = msg.description!!
    assertTrue(description.contains("Total runs: 10"))
    assertTrue(description.contains("First start:"))
    assertTrue(description.contains("Total duration: 300s"))
    assertTrue(description.contains("Run attempts:"))
    assertTrue(description.contains("Passed:"))
    assertTrue(description.contains("Failed:"))
  }
}
