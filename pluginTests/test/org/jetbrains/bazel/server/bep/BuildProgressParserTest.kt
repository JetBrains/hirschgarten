package org.jetbrains.bazel.server.bep

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BuildProgressParserTest {
  private val parser = BuildProgressParser()

  @Test
  fun `parses progress when present`() {
    val lines = listOf(
      "",
      "",
      "INFO: Analyzed 2 targets (127 packages loaded, 13908 targets configured).",
      "[1 / 4] checking cached actions",
      "",
    )
    val progress = parser.parse(lines)
    progress shouldBe BuildProgress(0.25, "checking cached actions")
  }

  @Test
  fun `returns nothing if no progress`() {
    val lines = listOf(
      "Computing main repo mapping: ",
      "",
    )
    val progress = parser.parse(lines)
    progress shouldBe null
  }

  @Test
  fun `surfaces analysis phase line instead of idle action counter`() {
    val analyzing = "Analyzing: 27402 targets (1 packages loaded, 204028 targets configured, 35602 aspect applications)"
    val lines = listOf(
      analyzing,
      "[1 / 1] no actions running",
    )
    parser.parse(lines) shouldBe BuildProgress(null, analyzing)
  }

  @Test
  fun `shows the moving action counter once the analysis phase line is frozen`() {
    val analyzing = "Analyzing: 27402 targets (13987 packages loaded, 822616 targets configured, 35602 aspect applications)"
    // first tick: the phase line advanced, so show it
    parser.parse(listOf(analyzing, "[52,207 / 52,663] checking cached actions")) shouldBe BuildProgress(null, analyzing)
    // next tick: identical (frozen) phase line, but the counter advanced -> show the whole counter line so it keeps moving
    parser.parse(listOf(analyzing, "[59,452 / 59,683] checking cached actions")) shouldBe BuildProgress(
      null,
      "[59,452 / 59,683] checking cached actions",
    )
  }

  @Test
  fun `keeps action progress while actions are running`() {
    // even with a remembered phase line, a non-idle action counter wins (execution progress)
    parser.parse(listOf("Analyzing: 2 targets (1 packages loaded, 2 targets configured)"))
    parser.parse(listOf("[100 / 200] 50 actions running")) shouldBe BuildProgress(0.5, "50 actions running")
  }
}
