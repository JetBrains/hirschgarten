package org.jetbrains.bazel.languages.projectview.parser

import org.jetbrains.bazel.languages.projectview.fixtures.ProjectViewParsingTestCase

class ProjectViewParsingBasicRulesTest : ProjectViewParsingTestCase("basicRules") {
  fun testDirectories() {
    doTest(true)
  }
}
