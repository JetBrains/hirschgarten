package org.jetbrains.bazel.languages.bazelquery.parser

import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelqueryParsingTestCase

class BazelqueryParsingTest : BazelqueryParsingTestCase("") {
  fun testExpr() = doTest(true, true)

  fun testFlag() = doTest(true, true)
}
