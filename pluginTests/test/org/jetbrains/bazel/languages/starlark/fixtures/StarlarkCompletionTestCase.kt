package org.jetbrains.bazel.languages.starlark.fixtures

import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach

abstract class StarlarkCompletionTestCase {
  protected val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1))

  @BeforeEach
  fun setUpTestDataPath() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("starlark/completion")
  }
}
