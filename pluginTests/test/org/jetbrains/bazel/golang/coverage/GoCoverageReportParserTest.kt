package org.jetbrains.bazel.golang.coverage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GoCoverageReportParserTest {
  @Test
  fun `parser converts Go cover profile blocks to per-line maximum hits`() {
    val coverage =
      parseCoverage(
        sequenceOf(
          "mode: atomic",
          "github.com/example/project/pkg/calculator.go:10.1,12.2 3 0",
          "github.com/example/project/pkg/calculator.go:11.3,13.4 2 5",
          "github.com/example/project/pkg/other.go:20.1,20.10 1 1",
        ),
      )

    coverage shouldBe
      mapOf(
        "github.com/example/project/pkg/calculator.go" to mapOf(
          10 to 0L,
          11 to 5L,
          12 to 5L,
          13 to 5L,
        ),
        "github.com/example/project/pkg/other.go" to mapOf(20 to 1L),
      )
  }

  @Test
  fun `parser reports and ignores malformed lines`() {
    val malformedLines = mutableListOf<String>()
    val coverage =
      parseCoverage(
        sequenceOf(
          "mode: set",
          "not a coverage block",
          "github.com/example/project/pkg/calculator.go:4.2,2.1 1 7",
          "github.com/example/project/pkg/calculator.go:5.2,5.10 1 1",
        ),
        malformedLines,
      )

    coverage shouldBe mapOf("github.com/example/project/pkg/calculator.go" to mapOf(5 to 1L))
    malformedLines shouldBe
      listOf(
        "not a coverage block",
        "github.com/example/project/pkg/calculator.go:4.2,2.1 1 7",
      )
  }

  @Test
  fun `parser accepts hit counts larger than Int max value`() {
    val coverage =
      parseCoverage(
        sequenceOf(
          "mode: atomic",
          "github.com/example/project/pkg/calculator.go:5.2,5.10 1 2147483648",
        ),
      )

    coverage shouldBe mapOf("github.com/example/project/pkg/calculator.go" to mapOf(5 to 2147483648L))
  }

  @Test
  fun `parser rejects unreasonably large coverage blocks`() {
    val malformedLines = mutableListOf<String>()
    val coverageLine = "github.com/example/project/pkg/calculator.go:1.1,2147483647.1 1 1"

    val coverage = parseCoverage(sequenceOf("mode: atomic", coverageLine), malformedLines)

    coverage shouldBe emptyMap()
    malformedLines shouldBe listOf(coverageLine)
  }

  @Test
  fun `parser ignores profiles without mode line`() {
    val coverage =
      parseCoverage(
        sequenceOf(
          "github.com/example/project/pkg/calculator.go:5.2,5.10 1 1",
        ),
      )

    coverage shouldBe emptyMap()
  }

  private fun parseCoverage(
    lines: Sequence<String>,
    malformedLines: MutableList<String> = mutableListOf(),
  ): Map<String, Map<Int, Long>> {
    val coverage = mutableMapOf<String, MutableMap<Int, Long>>()
    GoCoverageReportParser.parse(lines, malformedLines::add) { filePath, lineNumber, hits ->
      val lineHits = coverage.getOrPut(filePath) { mutableMapOf() }
      lineHits.merge(lineNumber, hits) { oldHits, newHits -> maxOf(oldHits, newHits) }
    }
    return coverage
  }
}
