package org.jetbrains.bazel.languages.starlark.commenter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

class StarlarkCommenterTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("starlark/commenter")

  fun testComment() = doTest()

  fun testUncomment() = doTest()

  private fun doTest() {
    VfsRootAccess.allowRootAccess(testRootDisposable, testDataPath)

    val name = getTestName(false)
    val source = "$name.bzl"
    myFixture.configureByFile(source)
    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
    val result = "${name}Result.bzl"
    myFixture.checkResultByFile(result)
  }
}
