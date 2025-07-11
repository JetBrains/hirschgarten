package org.jetbrains.bazel.languages.bazelquery.parser

import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryParsingTestCase

class BazelQueryParsingTest : BazelQueryParsingTestCase() {
  // simple just target test
  fun testJustTarget() = doTestWithDir("other/", true, true)

  // commands correct usage tests
  fun testExpr() = doTestWithDir("commands/", true, true)

  fun testTwoExprs() = doTestWithDir("commands/", true, true)

  fun testWordAndExpr() = doTestWithDir("commands/", true, true)

  fun testExprAndOptionalInt() = doTestWithDir("commands/", true, true)

  fun testRdeps() = doTestWithDir("commands/", true, true)

  fun testAttr() = doTestWithDir("commands/", true, true)

  fun testRbuildfiles() = doTestWithDir("commands/", true, true)

  // operations correct usage tests
  fun testLet() = doTestWithDir("operations/", true, true)

  fun testSet() = doTestWithDir("operations/", true, true)

  fun testOperationsOnSets() = doTestWithDir("operations/", true, true)

  // quotation correct usage tests
  fun testSingleQuotes() = doTestWithDir("other/", true, true)

  fun testDoubleQuotes() = doTestWithDir("other/", true, true)

  // quotation incorrect usage test
  fun testMixedQuotes() = doTestWithDir("other", true, false)

  // expression in additional parens test
  fun testUnnecessaryParens() = doTestWithDir("other", true, true)
}
