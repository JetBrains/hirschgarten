package org.jetbrains.bazel.languages.bazelrc.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class BazelrcParsingTest : BazelrcParsingTestCase("") {
  @Test
  fun testBasic() = doTest(true, true)

  @Test
  fun testQuoting() = doTest(true, true)

  @Test
  fun testValueWithQuoteString() = doTest(true, true)

  @Test
  fun testContinuations() = doTest(true, true)

  // the test case generates parsing errors on purpose
  @Test
  fun testImports() = doTest(true, false)
}
