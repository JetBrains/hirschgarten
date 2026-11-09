package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkStringAnnotatorTest {
  private val myFixture by bazelCodeInsightFixture(testDataDir = "starlark/annotation")

  @Test
  fun testStringAnnotator() {
    myFixture.configureByFile("StringAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }

  @Test
  fun testStringPrefixes() {
    myFixture.configureByFile("StringPrefixesAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }
}
