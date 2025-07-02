package org.jetbrains.bazel.languages.bazelquery.parser

import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryFlagsParsingTestCase

class BazelQueryFlagsParsingTest : BazelQueryFlagsParsingTestCase("") {
  // flags tests
  fun testNoValFlag() = doTest(true, true)

  fun testSpaceValFlag() = doTest(true, true)

  fun testEqualsValFlag() = doTest(true, true)

  fun testMixedValsFlags() = doTest(true, true)
}
