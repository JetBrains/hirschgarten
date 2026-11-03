package org.jetbrains.bazel.server.bep

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BuildProgressTimestampProbeTest {
  @Test
  fun `timestamp does not hide loading progress`() {
    val loading = "Loading: 12 packages loaded"
    val plain = BuildProgressParser().parse(listOf(loading))
    plain shouldBe BuildProgress(null, loading)
    BuildProgressParser().parse(listOf("(14:26:26) $loading")) shouldBe plain
  }

  @Test
  fun `timestamp does not hide analysis progress`() {
    val analyzing = "Analyzing: 2 targets (12 packages loaded, 120 targets configured)"
    val plain = BuildProgressParser().parse(listOf(analyzing))
    plain shouldBe BuildProgress(null, analyzing)
    BuildProgressParser().parse(listOf("(14:26:26) $analyzing")) shouldBe plain
  }

  @Test
  fun `timestamp does not replace analysis progress with an idle counter`() {
    val analyzing = "Analyzing: 2 targets (12 packages loaded, 120 targets configured)"
    val counter = "[1 / 1] no actions running"
    val plain = BuildProgressParser().parse(listOf(analyzing, counter))
    plain shouldBe BuildProgress(null, analyzing)
    BuildProgressParser().parse(listOf("(14:26:26) $analyzing", counter)) shouldBe plain
  }

  @Test
  fun `timestamp does not change execution progress`() {
    val action = "[1,000 / 2,000] 50 actions running"
    val plain = BuildProgressParser().parse(listOf(action))
    plain shouldBe BuildProgress(0.5, "50 actions running")
    BuildProgressParser().parse(listOf("(00:00:00) $action")) shouldBe plain
    BuildProgressParser().parse(listOf("(23:59:59) $action")) shouldBe plain
  }

  @Test
  fun `clock changes do not make a frozen analysis phase advance`() {
    val parser = BuildProgressParser()
    val analyzing = "Analyzing: 2 targets (12 packages loaded, 120 targets configured)"
    parser.parse(listOf("(14:26:26) $analyzing", "[2 / 4] checking cached actions")) shouldBe
      BuildProgress(null, analyzing)
    parser.parse(listOf("(14:26:27) $analyzing", "[3 / 4] checking cached actions")) shouldBe
      BuildProgress(null, "[3 / 4] checking cached actions")
  }

  @Test
  fun `timestamp text inside an action description is preserved`() {
    val details = "Generating snapshot (12:00:00) for //data:report"
    BuildProgressParser().parse(listOf("[1 / 4] $details")) shouldBe BuildProgress(0.25, details)
  }

  @Test
  fun `only the leading timestamp is removed`() {
    val details = "Generating snapshot (12:00:00) for //data:report"
    BuildProgressParser().parse(listOf("(14:26:26) [1 / 4] $details")) shouldBe BuildProgress(0.25, details)
  }
}
