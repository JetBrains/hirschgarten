package org.jetbrains.bazel.languages.starlark.annotation

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StarlarkFunctionAnnotatorTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String = "src/test/testData/starlark/annotation"

  fun test() {
    myFixture.configureByFile("FunctionAnnotatorTestData.bzl")
    myFixture.checkHighlighting(true, true, true)
  }
}
