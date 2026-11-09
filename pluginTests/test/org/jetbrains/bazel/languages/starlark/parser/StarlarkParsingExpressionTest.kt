package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.fixtures.starlarkParsingFixture
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingExpressionTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by starlarkParsingFixture(project, "expression")

  @Test
  fun testArguments() = parsing.doTest(true)

  @Test
  fun testBinary() = parsing.doTest(true)

  @Test
  fun testChains() = parsing.doTest(true)

  @Test
  fun testComprehension() = parsing.doTest(true)

  @Test
  fun testComprehensionWithLambdaCondition() = parsing.doTest(true)

  @Test
  fun testConditional() = parsing.doTest(true)

  @Test
  fun testDictEntries() = parsing.doTest(true)

  @Test
  fun testKeyword() = parsing.doTest(true)

  @Test
  fun testOperand() = parsing.doTest(true)

  @Test
  fun testPrecedence() = parsing.doTest(true)

  @Test
  fun testPrimary() = parsing.doTest(true)

  @Test
  fun testSlice() = parsing.doTest(true)

  @Test
  fun testTuple() = parsing.doTest(true)

  @Test
  fun testUnary() = parsing.doTest(true)
}
