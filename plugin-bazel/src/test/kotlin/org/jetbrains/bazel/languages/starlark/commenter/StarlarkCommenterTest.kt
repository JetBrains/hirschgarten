package org.jetbrains.bazel.languages.starlark.commenter

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.pathString

class StarlarkCommenterTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/starlark/commenter/").pathString

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
