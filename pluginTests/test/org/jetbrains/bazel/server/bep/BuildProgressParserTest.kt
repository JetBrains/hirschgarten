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
}
