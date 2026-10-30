package org.jetbrains.bazel.languages.bazelrc.commenter

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.readText
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcCommenterTest {
  private val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun setUpTestDataPath() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("bazelrc/commenter")
  }

  @Test
  fun testComment() = doTest("Comment")

  @Test
  fun testUncomment() = doTest("Uncomment")

  private fun doTest(name: String) {
    myFixture.configureByFile("$name.bazelrc")

    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)

    val result = myFixture.copyFileToProject("${name}Result.bazelrc")
    myFixture.checkResult(result.readText())
  }
}
