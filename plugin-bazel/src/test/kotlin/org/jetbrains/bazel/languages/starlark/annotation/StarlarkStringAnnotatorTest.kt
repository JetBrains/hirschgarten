package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkStringAnnotatorTest : StarlarkAnnotatorTestCase() {
  fun testFunctionAnnotator() {
    myFixture.configureByFile("StringAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }
}
