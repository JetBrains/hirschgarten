package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class StarlarkAnnotatorTestCase : BazelBasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("starlark/annotation")
}
