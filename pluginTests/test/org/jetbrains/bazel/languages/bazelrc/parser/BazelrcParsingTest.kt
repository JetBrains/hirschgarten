package org.jetbrains.bazel.languages.bazelrc.parser

import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcParsingTestCase

class BazelrcParsingTest : BazelrcParsingTestCase("") {
  fun testBasic() = doTest(true, true)

  fun testQuoting() = doTest(true, true)

  fun testValueWithQuoteString() = doTest(true, true)

  fun testContinuations() = doTest(true, true)

  // the test case generates parsing errors on purpose
  fun testImports() = doTest(true, false)
}
