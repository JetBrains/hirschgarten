package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingFunctionTest : StarlarkParsingTestCase("function") {
  @Test
  fun testDef() = doTest(true)

  @Test
  fun testKeywordOnlyBoundaryAtEnd() = doTest(true)

  @Test
  fun testLambda() = doTest(true)

  @Test
  fun testParameters() = doTest(true)
}
