package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingExpressionTest : StarlarkParsingTestCase("expression") {
  @Test
  fun testBinaryExpression() = doTest(true)

  @Test
  fun testIfExpression() = doTest(true)

  @Test
  fun testKeywordExpression() = doTest(true)

  @Test
  fun testLambdaExpression() = doTest(true)

  @Test
  fun testPrimaryExpression() = doTest(true)

  @Test
  fun testUnaryExpression() = doTest(true)
}
