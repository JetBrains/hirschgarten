package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingExpressionTest : StarlarkParsingTestCase("expression") {
  @Test
  fun testArguments() = doTest(true)

  @Test
  fun testBinary() = doTest(true)

  @Test
  fun testChains() = doTest(true)

  @Test
  fun testComprehension() = doTest(true)

  @Test
  fun testConditional() = doTest(true)

  @Test
  fun testDictEntries() = doTest(true)

  @Test
  fun testKeyword() = doTest(true)

  @Test
  fun testOperand() = doTest(true)

  @Test
  fun testPrecedence() = doTest(true)

  @Test
  fun testPrimary() = doTest(true)

  @Test
  fun testSlice() = doTest(true)

  @Test
  fun testTuple() = doTest(true)

  @Test
  fun testUnary() = doTest(true)
}
