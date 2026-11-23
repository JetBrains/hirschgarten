package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingBasicRulesTest : StarlarkParsingTestCase("basicRules") {
  @Test
  fun testArguments() = doTest(true)

  @Test
  fun testCompClause() = doTest(true)

  @Test
  fun testEntries() = doTest(true)

  @Test
  fun testOperand() = doTest(true)

  @Test
  fun testParameters() = doTest(true)

  @Test
  fun testKeywordOnlyBoundaryMisuse() = doTest(true)
}
