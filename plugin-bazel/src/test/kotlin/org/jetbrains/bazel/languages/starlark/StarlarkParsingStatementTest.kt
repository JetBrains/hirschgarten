package org.jetbrains.bazel.languages.starlark

class StarlarkParsingStatementTest : StarlarkParsingTestBase("statement") {
  fun testDefStatement() = doTest(true)
  fun testForStatement() = doTest(true)
  fun testIfStatement() = doTest(true)
  fun testSimpleStatement() = doTest(true)
}
