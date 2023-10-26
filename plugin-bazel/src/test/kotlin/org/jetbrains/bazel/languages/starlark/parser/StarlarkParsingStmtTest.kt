package org.jetbrains.bazel.languages.starlark.parser

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase

class StarlarkParsingStmtTest : StarlarkParsingTestCase("stmt") {
  fun testAssignStmt() = doTest(true)
  fun testBreakStmt() = doTest(true)
  fun testContinueStmt() = doTest(true)
  fun testExprStmt() = doTest(true)
  fun testLoadStmt() = doTest(true)
  fun testPassStmt() = doTest(true)
  fun testReturnStmt() = doTest(true)
}
