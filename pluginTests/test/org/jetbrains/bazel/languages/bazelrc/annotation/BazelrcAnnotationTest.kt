package org.jetbrains.bazel.languages.bazelrc.annotation

import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcAnnotatorTestCase

class BazelrcAnnotationTest : BazelrcAnnotatorTestCase() {
  fun testLabelFlags() {
    myFixture.configureByFile("LabelFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }

  fun testOldFlags() {
    myFixture.configureByFile("OldFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }

  fun testTargetFlags() {
    myFixture.configureByFile("TargetFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }
}
