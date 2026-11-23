package org.jetbrains.bazel.languages.projectview.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.projectview.fixtures.ProjectViewParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class ProjectViewParsingAllSectionsTest : ProjectViewParsingTestCase("sections") {
  @Test
  fun testDirectories() {
    doTest(true)
  }

  @Test
  fun testTargets() {
    doTest(true)
  }

  @Test
  fun testImport() {
    doTest(true)
  }

  @Test
  fun testMinimal() {
    doTest(true)
  }

  @Test
  fun testIndex() {
    doTest(true)
  }

  @Test
  fun testShardSync() {
    doTest(true)
  }

  @Test
  fun testTargetShardSize() {
    doTest(true)
  }

  @Test
  fun testListValueInTheSameLine() {
    doTest(true)
  }

  @Test
  fun testSingleListValueInTheSameLine() {
    doTest(true)
  }

  @Test
  fun testWrongSyntax() {
    doTest(true)
  }
}
