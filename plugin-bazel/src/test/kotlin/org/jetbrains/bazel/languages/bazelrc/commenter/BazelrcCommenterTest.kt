package org.jetbrains.bazel.languages.bazelrc.commenter

import com.google.idea.testing.runfiles.Runfiles
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.io.path.pathString

class BazelrcCommenterTest : BasePlatformTestCase() {
  override fun getTestDataPath(): String = Runfiles.runfilesPath("/plugin-bazel/src/test/testData/bazelrc/commenter/").pathString

  fun testComment() = doTest()

  fun testUncomment() = doTest()

  private fun doTest() {
    val name = getTestName(false)
    val source = "$name.bazelrc"
    myFixture.configureByFile(source)
    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
    val result = "${name}Result.bazelrc"
    myFixture.checkResultByFile(result)
  }
}
