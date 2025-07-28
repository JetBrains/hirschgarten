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

  fun testMinimal() {
    doTest(true)
  }

  fun testShardSync() {
    doTest(true)
  }

  fun testTargetShardSize() {
    doTest(true)
  }

  fun testListValueInTheSameLine() {
    doTest(true)
  }

  fun testSingleListValueInTheSameLine() {
    doTest(true)
  }
}
