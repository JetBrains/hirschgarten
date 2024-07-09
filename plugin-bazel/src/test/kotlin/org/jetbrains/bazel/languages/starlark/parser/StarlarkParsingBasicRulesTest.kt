package org.jetbrains.bazel.languages.starlark.parser

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase

class StarlarkParsingBasicRulesTest : StarlarkParsingTestCase("basicRules") {
  fun testArguments() = doTest(true)
  fun testCompClause() = doTest(true)
  fun testEntries() = doTest(true)
  fun testOperand() = doTest(true)
  fun testParameters() = doTest(true)
}
