package org.jetbrains.bazel.languages.starlark.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class StarlarkReferencesTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("starlark/references")
}
