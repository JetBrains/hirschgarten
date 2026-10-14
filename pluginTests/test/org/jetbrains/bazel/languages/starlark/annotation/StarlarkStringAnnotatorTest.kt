package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkStringAnnotatorTest : StarlarkAnnotatorTestCase() {
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
