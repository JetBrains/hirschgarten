package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkFunctionAnnotatorTest : StarlarkAnnotatorTestCase() {
  fun testFunctionAnnotator() {
    myFixture.configureByFile("FunctionAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }

  fun testUnresolvedNamedArgumentsAnnotator() {
    myFixture.configureByFile("UnresolvedNamedArgumentsAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, false, false)
  }
}
