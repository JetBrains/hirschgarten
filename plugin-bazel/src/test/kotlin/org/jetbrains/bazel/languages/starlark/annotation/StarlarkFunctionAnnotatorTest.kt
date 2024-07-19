package org.jetbrains.bazel.languages.starlark.annotation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkAnnotatorTestCase

class StarlarkFunctionAnnotatorTest : StarlarkAnnotatorTestCase() {
  fun testFunctionAnnotator() {
    myFixture.configureByFile("FunctionAnnotatorTestData.bzlmock")
    myFixture.checkHighlighting(true, true, true)
  }
}
