package org.jetbrains.bazel.languages.projectview.parser

import org.jetbrains.bazel.languages.projectview.fixtures.ProjectViewParsingTestCase

class ProjectViewParsingAllSectionsTest : ProjectViewParsingTestCase("sections") {
  fun testDirectories() {
    doTest(true)
  }

  fun testTargets() {
    doTest(true)
  }

  fun testImport() {
    doTest(true)
  }

  fun testWorkspaceType() {
    doTest(true)
  }

  fun testAdditionalLanguages() {
    doTest(true)
  }

  fun testJavaLanguageLevel() {
    doTest(true)
  }

  fun testJavaLanguageLevelPreview() {
    doTest(true)
  }

  fun testTestSources() {
    doTest(true)
  }

  fun testShardSync() {
    doTest(true)
  }

  fun testTargetShardSize() {
    doTest(true)
  }

  fun testExcludeLibrary() {
    doTest(true)
  }
}
