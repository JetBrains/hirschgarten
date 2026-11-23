package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class BazelQueryParsingTest : BazelQueryParsingTestCase() {
  // simple just target test
  @Test
  fun testJustTarget() = doTestWithDir("other/", true, true)

  // commands correct usage tests
  @Test
  fun testExpr() = doTestWithDir("commands/", true, true)

  @Test
  fun testTwoExprs() = doTestWithDir("commands/", true, true)

  @Test
  fun testWordAndExpr() = doTestWithDir("commands/", true, true)

  @Test
  fun testExprAndOptionalInt() = doTestWithDir("commands/", true, true)

  @Test
  fun testRdeps() = doTestWithDir("commands/", true, true)

  @Test
  fun testAttr() = doTestWithDir("commands/", true, true)

  @Test
  fun testRbuildfiles() = doTestWithDir("commands/", true, true)

  // operations correct usage tests
  @Test
  fun testLet() = doTestWithDir("operations/", true, true)

  @Test
  fun testSet() = doTestWithDir("operations/", true, true)

  @Test
  fun testOperationsOnSets() = doTestWithDir("operations/", true, true)

  // quotation correct usage tests
  @Test
  fun testSingleQuotes() = doTestWithDir("other/", true, true)

  @Test
  fun testDoubleQuotes() = doTestWithDir("other/", true, true)

  // quotation incorrect usage test
  @Test
  fun testMixedQuotes() = doTestWithDir("other", true, false)

  // expression in additional parens test
  @Test
  fun testUnnecessaryParens() = doTestWithDir("other", true, true)
}
