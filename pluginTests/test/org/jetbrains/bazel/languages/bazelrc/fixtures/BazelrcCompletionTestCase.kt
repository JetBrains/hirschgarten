package org.jetbrains.bazel.languages.bazelrc.fixtures

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

abstract class BazelrcCompletionTestCase : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelrc/completion")
}
