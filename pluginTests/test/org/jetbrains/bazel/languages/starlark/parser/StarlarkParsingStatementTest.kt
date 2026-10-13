package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingStatementTest : StarlarkParsingTestCase("statement") {
  @Test
  fun testDefStatement() = doTest(true)

  @Test
  fun testForStatement() = doTest(true)

  @Test
  fun testIfStatement() = doTest(true)

  @Test
  fun testSimpleStatement() = doTest(true)
}
