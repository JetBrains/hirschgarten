package org.jetbrains.bsp.bazel.languages.starlark

class StarlarkParsingStmtTest : StarlarkParsingTestBase("stmt") {
    fun testAssignStmt() = doTest(true)
    fun testBreakStmt() = doTest(true)
    fun testContinueStmt() = doTest(true)
    fun testExprStmt() = doTest(true)
    fun testLoadStmt() = doTest(true)
    fun testPassStmt() = doTest(true)
    fun testReturnStmt() = doTest(true)
}