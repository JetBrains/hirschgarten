package org.jetbrains.bazel.languages.starlark.parser

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase

class StarlarkParsingExpressionTest : StarlarkParsingTestCase("expression") {
  fun testBinaryExpression() = doTest(true)
  fun testIfExpression() = doTest(true)
  fun testLambdaExpression() = doTest(true)
  fun testPrimaryExpression() = doTest(true)
  fun testUnaryExpression() = doTest(true)
}
