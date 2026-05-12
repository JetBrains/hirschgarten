package org.jetbrains.bazel.info

import com.google.protobuf.TextFormat
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.junit.jupiter.api.Test

class BspTargetInfoTest {
  @Test
  fun `Scala target info parses test suite metadata from textproto`() {
    val builder = TargetInfo.newBuilder()

    TextFormat.merge(
      """
      scala_target_info {
        test_suite_class: "io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite"
        test_suite_label: "@rules_scala//src/java/io/bazel/rulesscala/specs2:specs2_test_discovery"
      }
      """.trimIndent(),
      builder,
    )

    builder.build().scalaTargetInfo.testSuiteClass shouldBe
      "io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite"
    builder.build().scalaTargetInfo.testSuiteLabel shouldBe
      "@rules_scala//src/java/io/bazel/rulesscala/specs2:specs2_test_discovery"
  }
}
