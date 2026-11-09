package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.fixtures.starlarkParsingFixture
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingComplexTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by starlarkParsingFixture(project, "complex")

  @Test
  fun testAspects() = parsing.doTest(true, true)

  @Test
  fun testBuild() = parsing.doTest(true, true)
}
