package org.jetbrains.bazel.languages.bazelrc.fixtures

import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach

abstract class BazelrcAnnotatorTestCase {
  protected val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun setUpTestDataPath() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("bazelrc/annotation")
  }
}
