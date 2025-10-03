package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.Aborted.AbortReason
import com.intellij.build.events.MessageEvent
import com.intellij.testFramework.junit5.TestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@TestApplication
class AbortedParserTest {

  private val parser = AbortedParser()

  @Test
  fun `returns null for SKIPPED aborted events`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.SKIPPED,
      description = "Target was skipped"
    ).build()

    val result = parser.parse(event)

    assertNull(result, "Skipped targets should not generate events")
  }

  @Test
  fun `parses INTERNAL abort`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.INTERNAL,
      description = "Target depends on failed target //dep:library"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message?.contains("INTERNAL") == true)
    assertTrue(msg.message?.contains("//pkg:target") == true)
    assertTrue(msg.description?.contains("//dep:library") == true)
  }

  @Test
  fun `parses ANALYSIS_FAILURE abort`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.ANALYSIS_FAILURE,
      description = "Analysis of target failed due to missing dependency"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message?.contains("ANALYSIS_FAILURE") == true)
    assertTrue(msg.message?.contains("//pkg:target") == true)
    assertTrue(msg.description?.contains("missing dependency") == true)
  }

  @Test
  fun `parses NO_ANALYZE abort`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.NO_ANALYZE,
      description = "Target was not analyzed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message?.contains("NO_ANALYZE") == true)
    assertTrue(msg.description?.contains("Target was not analyzed") == true)
  }

  @Test
  fun `parses NO_BUILD abort`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.NO_BUILD,
      description = "Target was not built"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message?.contains("NO_BUILD") == true)
    assertTrue(msg.description?.contains("Target was not built") == true)
  }

  @Test
  fun `parses LOADING_FAILURE abort`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.LOADING_FAILURE,
      description = "Failed to load BUILD file"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertEquals(MessageEvent.Kind.ERROR, msg.kind)
    assertTrue(msg.message?.contains("LOADING_FAILURE") == true)
    assertTrue(msg.description?.contains("BUILD file") == true)
  }

  @Test
  fun `handles abort with empty description`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.INTERNAL,
      description = ""
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    // Should still create message with reason in title
    assertTrue(msg.message?.contains("INTERNAL") == true)
    // Description should fall back to generated message
    assertTrue(msg.description?.contains("Could not complete target: //pkg:target") == true)
  }

  @Test
  fun `handles abort with multiline description`() {
    val multilineDesc = """
      Target build failed:
      - Missing dependency: //foo:bar
      - Configuration error in //baz:qux
      - Circular dependency detected
    """.trimIndent()

    val event = BepTestUtils.aborted(
      label = "//pkg:target",
      reason = AbortReason.ANALYSIS_FAILURE,
      description = multilineDesc
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.description?.contains("Missing dependency: //foo:bar") == true)
    assertTrue(msg.description?.contains("Configuration error") == true)
    assertTrue(msg.description?.contains("Circular dependency") == true)
  }

  @Test
  fun `handles invalid label format gracefully`() {
    val event = BepTestUtils.aborted(
      label = "invalid:::label",
      reason = AbortReason.INTERNAL,
      description = "Failed"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    // Should still create event despite invalid label
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.message?.contains("invalid:::label") == true)
  }

  @Test
  fun `handles dependency chain in description`() {
    val event = BepTestUtils.aborted(
      label = "//top:target",
      reason = AbortReason.INTERNAL,
      description = "Target //top:target failed because //mid:lib failed because //bottom:dep failed to build"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent
    assertTrue(msg.description?.contains("//top:target") == true)
    assertTrue(msg.description?.contains("//mid:lib") == true)
    assertTrue(msg.description?.contains("//bottom:dep") == true)
  }

  @Test
  fun `formats message correctly`() {
    val event = BepTestUtils.aborted(
      label = "//pkg:my_target",
      reason = AbortReason.INTERNAL,
      description = "Dependency //other:lib failed to compile"
    ).build()

    val result = parser.parse(event)

    assertNotNull(result)
    val msg = result as com.intellij.build.events.MessageEvent

    // Check title format: "{reason}: {label}"
    assertEquals("INTERNAL: //pkg:my_target", msg.message)

    // Check description contains the provided error message
    val description = msg.description
    assertTrue(description?.contains("Dependency //other:lib failed to compile") == true)
  }

  @Test
  fun `returns null for non-aborted events`() {
    val event = BepTestUtils.testResult(
      label = "//test:unit_test",
      status = com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TestStatus.PASSED
    ).build()

    val result = parser.parse(event)

    assertNull(result)
  }

  @Test
  fun `returns null for events without aborted payload`() {
    val event = com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent.newBuilder()
      .setId(
        com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId.newBuilder()
          .setTargetCompleted(
            com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId.TargetCompletedId.newBuilder()
              .setLabel("//pkg:target")
          )
      )
      // No .setAborted()
      .build()

    val result = parser.parse(event)

    assertNull(result)
  }
}
