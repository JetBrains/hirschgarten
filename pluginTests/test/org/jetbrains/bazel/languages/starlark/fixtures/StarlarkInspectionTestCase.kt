package org.jetbrains.bazel.languages.starlark.fixtures

import org.jetbrains.bazel.test.framework.BazelBasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class StarlarkInspectionTestCase : BazelBasePlatformTestCase() {
  override fun getTestDataPath(): String = BazelPathManager.getTestFixture("starlark/inspection")
}
