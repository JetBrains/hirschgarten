package org.jetbrains.bsp.bazel.languages.starlark

class StarlarkParsingBasicRulesTest : StarlarkParsingTestBase("basicRules") {
    fun testArguments() = doTest(true)
    fun testCompClause() = doTest(true)
    fun testEntries() = doTest(true)
    fun testOperand() = doTest(true)
    fun testParameters() = doTest(true)
}