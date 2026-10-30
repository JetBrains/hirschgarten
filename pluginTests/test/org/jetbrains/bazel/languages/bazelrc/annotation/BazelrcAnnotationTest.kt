package org.jetbrains.bazel.languages.bazelrc.annotation

import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcAnnotatorTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcAnnotationTest : BazelrcAnnotatorTestCase() {
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
