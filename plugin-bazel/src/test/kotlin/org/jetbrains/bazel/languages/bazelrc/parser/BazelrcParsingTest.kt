package org.jetbrains.bazel.languages.bazelrc.parser

import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcParsingTestCase

class BazelrcParsingTest : BazelrcParsingTestCase("") {
  fun testBasic() = doTest(true, true)

  fun testQuoting() = doTest(true, true)
}
