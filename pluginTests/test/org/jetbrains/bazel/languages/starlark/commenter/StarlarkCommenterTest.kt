package org.jetbrains.bazel.languages.starlark.commenter

import com.intellij.openapi.actionSystem.IdeActions
import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class StarlarkCommenterTest {
  private val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun beforeEach() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("starlark/commenter")
  }

  @Test
  fun testComment() = doTest("Comment")

  @Test
  fun testUncomment() = doTest("Uncomment")

  private fun doTest(name: String) {
    myFixture.configureByFile("$name.bzl")
    myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
    myFixture.checkResultByFile("${name}Result.bzl")
  }
}
