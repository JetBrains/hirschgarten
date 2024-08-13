package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class StarlarkAnnotatorTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = "src/test/testData/starlark/annotation"
}
