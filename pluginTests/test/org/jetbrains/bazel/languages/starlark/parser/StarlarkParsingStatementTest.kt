package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.fixtures.starlarkParsingFixture
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingStatementTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by starlarkParsingFixture(project, "statement")

  @Test
  fun testAssignment() = parsing.doTest(true)

  @Test
  fun testAssignmentTargets() = parsing.doTest(true)

  @Test
  fun testBlockComments() = parsing.doTest(true)

  @Test
  fun testBreak() = parsing.doTest(true)

  @Test
  fun testColonAtLineEndInBrackets() = parsing.doTest(true)

  @Test
  fun testComments() = parsing.doTest(true)

  @Test
  fun testContinue() = parsing.doTest(true)

  @Test
  fun testExpression() = parsing.doTest(true)

  @Test
  fun testFor() = parsing.doTest(true)

  @Test
  fun testIf() = parsing.doTest(true)

  @Test
  fun testIndentation() = parsing.doTest(true)

  @Test
  fun testLoad() = parsing.doTest(true)

  @Test
  fun testPass() = parsing.doTest(true)

  @Test
  fun testReturn() = parsing.doTest(true)

  @Test
  fun testSemicolons() = parsing.doTest(true)

  @Test
  fun testSimple() = parsing.doTest(true)
}
