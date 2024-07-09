package org.jetbrains.bazel.languages.starlark.commenter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StarlarkCommenterTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/starlark/commenter"
    fun testComment() = doTest()
    fun testUncomment() = doTest()

    private fun doTest() {
        val name = getTestName(false)
        val source = "$name.bzl"
        myFixture.configureByFile(source)
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        val result = "${name}Result.bzl"
        myFixture.checkResultByFile(result)
    }
}
