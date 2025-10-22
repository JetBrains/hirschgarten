package org.jetbrains.bazel.languages.bazelrc.commenter

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.test.framework.BazelPathManager
import kotlin.io.path.pathString

class BazelrcCommenterTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String =
    BazelPathManager.getTestFixture("bazelrc/commenter")

  fun testComment() = doTest()

  fun testUncomment() = doTest()

  private fun doTest() {
    VfsRootAccess.allowRootAccess(this.testRootDisposable, this.testDataPath)

    val name = getTestName(false)

    myFixture.configureByFile("$name.bazelrc")

    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

    val result = myFixture.copyFileToProject("${name}Result.bazelrc")
    myFixture.checkResult(result.readText())
  }
}
