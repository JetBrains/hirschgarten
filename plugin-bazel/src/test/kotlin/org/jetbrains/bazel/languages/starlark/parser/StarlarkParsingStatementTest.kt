package org.jetbrains.bazel.languages.starlark.parser

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase

class StarlarkParsingStatementTest : StarlarkParsingTestCase("statement") {
  fun testDefStatement() = doTest(true)
  fun testForStatement() = doTest(true)
  fun testIfStatement() = doTest(true)
  fun testSimpleStatement() = doTest(true)
}
