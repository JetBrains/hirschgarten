package org.jetbrains.bazel.run.test

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.test.framework.target.TestBuildTargetFactory
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.junit.jupiter.api.Test

class BazelTestFilterNormalizerTest {
  @Test
  fun `null filter stays null`() {
    normalizeBazelTestFilter(specs2Target(), null, emptyList(), useJetBrainsTestRunner = false) shouldBe null
  }

  @Test
  fun `JetBrains test runner keeps raw filter`() {
    normalizeBazelTestFilter(specs2Target(), "com.example.FieldMasksTest", emptyList(), useJetBrainsTestRunner = true) shouldBe
      "com.example.FieldMasksTest"
  }

  @Test
  fun `non Specs2 target keeps raw filter`() {
    normalizeBazelTestFilter(scalaTarget(), "com.example.FieldMasksTest", emptyList(), useJetBrainsTestRunner = false) shouldBe
      "com.example.FieldMasksTest"
  }

  @Test
  fun `Specs2 discovered suite rewrites class filter`() {
    normalizeBazelTestFilter(specs2Target(), "com.example.FieldMasksTest", emptyList(), useJetBrainsTestRunner = false) shouldBe
      "^\\Qcom.example.FieldMasksTest\\E(#.*)?$"
  }

  @Test
  fun `Specs2 discovered suite can be identified by discovery label`() {
    normalizeBazelTestFilter(specs2TargetByLabel(), "com.example.FieldMasksTest", emptyList(), useJetBrainsTestRunner = false) shouldBe
      "^\\Qcom.example.FieldMasksTest\\E(#.*)?$"
  }

  @Test
  fun `Specs2 discovered suite can be identified by canonical bzlmod discovery label`() {
    normalizeBazelTestFilter(
      specs2TargetByLabel("@@rules_scala~//src/java/io/bazel/rulesscala/specs2:specs2_test_discovery"),
      "com.example.FieldMasksTest",
      emptyList(),
      useJetBrainsTestRunner = false,
    ) shouldBe "^\\Qcom.example.FieldMasksTest\\E(#.*)?$"
  }

  @Test
  fun `extra test arguments keep raw filter`() {
    normalizeBazelTestFilter(specs2Target(), "com.example.FieldMasksTest", listOf("-t", "example"), useJetBrainsTestRunner = false) shouldBe
      "com.example.FieldMasksTest"
  }

  @Test
  fun `regex like filter stays raw`() {
    normalizeBazelTestFilter(specs2Target(), "com.example.FieldMasksTest#.*", emptyList(), useJetBrainsTestRunner = false) shouldBe
      "com.example.FieldMasksTest#.*"
  }

  private fun specs2Target() =
    scalaTarget(
      ScalaBuildTarget(
        scalaVersion = "2.13.16",
        sdkJars = emptyList(),
        scalacOptions = emptyList(),
        testSuiteClass = "io.bazel.rulesscala.specs2.Specs2DiscoveredTestSuite",
      ),
    )

  private fun specs2TargetByLabel(
    label: String = "@rules_scala//src/java/io/bazel/rulesscala/specs2:specs2_test_discovery",
  ) =
    scalaTarget(
      ScalaBuildTarget(
        scalaVersion = "2.13.16",
        sdkJars = emptyList(),
        scalacOptions = emptyList(),
        testSuiteLabel = label,
      ),
    )

  private fun scalaTarget(
    data: BuildTargetData = ScalaBuildTarget(
      scalaVersion = "2.13.16",
      sdkJars = emptyList(),
      scalacOptions = emptyList(),
    ),
  ) = TestBuildTargetFactory.createSimpleTarget(
    id = Label.parse("//test:specs2"),
    kind = "scala_junit_test",
    ruleType = RuleType.TEST,
    languages = setOf(LanguageClass.JAVA, LanguageClass.SCALA),
    data = listOf(data),
  )
}
