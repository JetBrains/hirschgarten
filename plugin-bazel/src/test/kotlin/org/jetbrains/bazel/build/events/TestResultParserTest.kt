package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TestStatus
import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@TestApplication
class TestResultParserTest {

  private val parser = TestResultParser()

  @Test
  fun `returns null for passed test`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.PASSED
    ).build()

    val result = parser.parse(event)

    assertNull(result, "Passed tests should not generate events")
  }

  @Test
  fun `returns null for NO_STATUS test`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.NO_STATUS
    ).build()

    val result = parser.parse(event)

    assertNull(result)
  }

  @Test
  fun `parses failed test`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED,
      statusDetails = "Expected 42 but was 0"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test FAILED"))
    assertTrue(msg.message.contains("//test:unit_test"))
    assertTrue(msg.description!!.contains("Expected 42 but was 0"))
  }

  @Test
  fun `parses timeout test`() {
    val event = BepTestUtils.testResult(
      label = "//test:slow_test",
      status = TestStatus.TIMEOUT,
      statusDetails = "Test exceeded timeout of 60s"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test TIMEOUT"))
    assertTrue(msg.description!!.contains("exceeded timeout"))
  }

  @Test
  fun `parses flaky test as warning`() {
    val event = BepTestUtils.testResult(
      label = "//test:flaky_test",
      status = TestStatus.FLAKY,
      statusDetails = "Test passed on retry"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.WARNING, msg.kind)
    assertTrue(msg.message.contains("Test FLAKY"))
  }

  @Test
  fun `parses incomplete test`() {
    val event = BepTestUtils.testResult(
      label = "//test:incomplete_test",
      status = TestStatus.INCOMPLETE,
      statusDetails = "Test was interrupted"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test INCOMPLETE"))
  }

  @Test
  fun `parses failed_to_build test`() {
    val event = BepTestUtils.testResult(
      label = "//test:broken_test",
      status = TestStatus.FAILED_TO_BUILD,
      statusDetails = "Compilation failed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test FAILED_TO_BUILD"))
  }

  @Test
  fun `includes test duration when available`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED,
      durationMillis = 1234
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.description!!.contains("Duration: 1234ms"))
  }

  @Test
  fun `handles test without status details`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    // Should still create message without crashing
    assertTrue(msg.description!!.contains("Test FAILED"))
  }

  @Test
  fun `handles empty status details`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED,
      statusDetails = ""
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    // Should not include empty details
    assertFalse(msg.description!!.lines().any { it.trim().isEmpty() && it != msg.description!!.lines().last() })
  }

  @Test
  fun `handles invalid label format gracefully`() {
    val event = BepTestUtils.testResult(
      label = "invalid:::label",
      status = TestStatus.FAILED,
      statusDetails = "Test failed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    // Should still create event despite invalid label
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.message.contains("invalid:::label"))
  }

  @Test
  fun `handles remote_failure status`() {
    val event = BepTestUtils.testResult(
      label = "//test:remote_test",
      status = TestStatus.REMOTE_FAILURE,
      statusDetails = "Remote execution failed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test REMOTE_FAILURE"))
  }

  @Test
  fun `handles tool_halted_before_testing status`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.TOOL_HALTED_BEFORE_TESTING,
      statusDetails = "Build was interrupted"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message.contains("Test TOOL_HALTED_BEFORE_TESTING"))
  }

  @Test
  fun `includes multiline status details`() {
    val multilineDetails = """
      Test assertion failed:
        Expected: <42>
        Actual: <0>
      at TestClass.testMethod(TestClass.java:123)
    """.trimIndent()

    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED,
      statusDetails = multilineDetails
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as MessageEvent
    assertTrue(msg.description!!.contains("Expected: <42>"))
    assertTrue(msg.description!!.contains("Actual: <0>"))
    assertTrue(msg.description!!.contains("TestClass.java:123"))
  }

  @Test
  fun `handles test with output files`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = TestStatus.FAILED,
      outputFiles = listOf(
        "/tmp/test_output.xml",
        "/tmp/test_log.txt"
      ),
      statusDetails = "Test failed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    // Parser should handle output files presence gracefully
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
  }
}
