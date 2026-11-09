package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.fixtures.starlarkParsingFixture
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingFunctionTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by starlarkParsingFixture(project, "function")

  @Test
  fun testDef() = parsing.doTest(true)

  @Test
  fun testKeywordOnlyBoundaryAtEnd() = parsing.doTest(true)

  @Test
  fun testLambda() = parsing.doTest(true)

  @Test
  fun testParameters() = parsing.doTest(true)
}
