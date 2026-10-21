package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryFlagsParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class BazelQueryFlagsParsingTest : BazelQueryFlagsParsingTestCase("") {
  @Test
  fun testNoValFlag() = doTest(true, true)

  @Test
  fun testSpaceValFlag() = doTest(true, true)

  @Test
  fun testEqualsValFlag() = doTest(true, true)

  @Test
  fun testMixedValsFlags() = doTest(true, true)
}
