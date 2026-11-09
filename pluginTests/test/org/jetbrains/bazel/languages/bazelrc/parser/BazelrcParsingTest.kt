package org.jetbrains.bazel.languages.bazelrc.parser

import com.intellij.platform.testFramework.junit5.codeInsight.fixture.parsingFixture
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.junit.jupiter.api.Test

@TestApplication
class BazelrcParsingTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by parsingFixture(
    project,
    BazelPathManager.getTestFixture("bazelrc/parser"),
    "bazelrc",
    definitions = arrayOf(BazelrcParserDefinition()),
  )

  @Test
  fun testBasic() = parsing.doTest(true, true)

  @Test
  fun testQuoting() = parsing.doTest(true, true)

  @Test
  fun testValueWithQuoteString() = parsing.doTest(true, true)

  @Test
  fun testContinuations() = parsing.doTest(true, true)

  // the test case generates parsing errors on purpose
  @Test
  fun testImports() = parsing.doTest(true, false)
}
