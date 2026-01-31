package org.jetbrains.bazel.languages.projectview.commenter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager

class ProjectViewCommenterTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("projectview/commenter")

  fun testComment() = doTest()

  fun testUncomment() = doTest()

  fun testUncommentNoSpace() = doTest()

  private fun doTest() {
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)

    val name = getTestName(false)

    myFixture.configureByFile("$name.bazelproject")

    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

    val result = myFixture.copyFileToProject("${name}Result.bazelproject")
    myFixture.checkResult(result.readText())
  }
}
