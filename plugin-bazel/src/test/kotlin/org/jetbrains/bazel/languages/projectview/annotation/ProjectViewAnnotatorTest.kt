package org.jetbrains.bazel.languages.projectview.annotation

import org.jetbrains.bazel.languages.projectview.fixtures.ProjectViewAnnotatorTestCase

class ProjectViewAnnotatorTest : ProjectViewAnnotatorTestCase() {
  fun test() {
    myFixture.configureByFile("ProjectViewAnnotatorTestData.bazelproject")
    myFixture.checkHighlighting(true, true, true)
  }
}
