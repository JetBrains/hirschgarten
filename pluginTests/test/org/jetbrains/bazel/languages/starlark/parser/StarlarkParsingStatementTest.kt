package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingStatementTest : StarlarkParsingTestCase("statement") {
  @Test
  fun testAssignment() = doTest(true)

  @Test
  fun testAssignmentTargets() = doTest(true)

  @Test
  fun testBlockComments() = doTest(true)

  @Test
  fun testBreak() = doTest(true)

  @Test
  fun testComments() = doTest(true)

  @Test
  fun testContinue() = doTest(true)

  @Test
  fun testExpression() = doTest(true)

  @Test
  fun testFor() = doTest(true)

  @Test
  fun testIf() = doTest(true)

  @Test
  fun testIndentation() = doTest(true)

  @Test
  fun testLoad() = doTest(true)

  @Test
  fun testPass() = doTest(true)

  @Test
  fun testReturn() = doTest(true)

  @Test
  fun testSemicolons() = doTest(true)

  @Test
  fun testSimple() = doTest(true)
}
