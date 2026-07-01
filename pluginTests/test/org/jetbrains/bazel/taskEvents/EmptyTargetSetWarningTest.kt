package org.jetbrains.bazel.taskEvents

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EmptyTargetSetWarningTest {
  @Test
  fun `matches every line of the empty target set warning`() {
    isEmptyTargetSetWarning("WARNING: Usage: bazel build <options> <targets>.") shouldBe true
    isEmptyTargetSetWarning("Invoke `bazel help build` for full description of usage and options.") shouldBe true
    isEmptyTargetSetWarning("Your request is correct, but requested an empty set of targets. Nothing will be built.") shouldBe true
  }

  @Test
  fun `does not match the analysis-cache discard warning`() {
    isEmptyTargetSetWarning(
      "WARNING: Build option --javacopt has changed, discarding analysis cache (this can be expensive, see ...).",
    ) shouldBe false
  }

  @Test
  fun `does not match ordinary build output`() {
    isEmptyTargetSetWarning("INFO: Analyzed 48 targets (241 packages loaded, 22287 targets configured).") shouldBe false
    isEmptyTargetSetWarning("INFO: Build completed successfully, 1 total action") shouldBe false
  }
}
