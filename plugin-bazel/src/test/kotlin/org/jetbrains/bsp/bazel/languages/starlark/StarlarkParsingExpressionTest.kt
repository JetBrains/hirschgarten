package org.jetbrains.bsp.bazel.languages.starlark

class StarlarkParsingExpressionTest : StarlarkParsingTestBase("expression") {
  fun testBinaryExpression() = doTest(true)
  fun testIfExpression() = doTest(true)
  fun testLambdaExpression() = doTest(true)
  fun testPrimaryExpression() = doTest(true)
  fun testUnaryExpression() = doTest(true)
}
