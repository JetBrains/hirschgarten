package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingStmtTest : StarlarkParsingTestCase("stmt") {
  @Test
  fun testAssignStmt() = doTest(true)

  @Test
  fun testBreakStmt() = doTest(true)

  @Test
  fun testContinueStmt() = doTest(true)

  @Test
  fun testExprStmt() = doTest(true)

  @Test
  fun testLoadStmt() = doTest(true)

  @Test
  fun testPassStmt() = doTest(true)

  @Test
  fun testReturnStmt() = doTest(true)
}
