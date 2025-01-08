package org.jetbrains.bazel.languages.bazelrc.annotation

import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcAnnotatorTestCase

class BazelrcAnnotationTest : BazelrcAnnotatorTestCase() {
  fun testLabelFlags() {
    myFixture.configureByFile("LabelFlagsTestData.bazelrc")
    myFixture.checkHighlighting(true, true, true)
  }
}
