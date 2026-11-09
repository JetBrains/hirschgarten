package org.jetbrains.bazel.languages.bazelrc.annotation

import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcAnnotationTest {
  private val myFixture by bazelCodeInsightFixture(testDataDir = "bazelrc/annotation")

  @Test
  fun testLabelFlags() {
    myFixture.configureByFile("LabelFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }

  @Test
  fun testOldFlags() {
    myFixture.configureByFile("OldFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }

  @Test
  fun testTargetFlags() {
    myFixture.configureByFile("TargetFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }
}
