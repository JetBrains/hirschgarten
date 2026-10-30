package org.jetbrains.bazel.languages.bazelrc.quickfix

import org.jetbrains.bazel.test.framework.BazelPathManager
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcQuickFixTest {
  private val myFixture by bazelCodeInsightFixture()

  @BeforeEach
  fun setUpTestDataPath() {
    myFixture.testDataPath = BazelPathManager.getTestFixture("bazelrc/quickfix")
  }

  @Test
  fun testDeleteNoOpFlag() = doTest("DeleteNoOpFlag")

  @Test
  fun testReplaceDeprecatedFlag() = doTest("ReplaceDeprecatedFlag")

  @Test
  fun testReplaceNegatedDeprecatedFlag() = doTest("ReplaceNegatedDeprecatedFlag")

  private fun doTest(name: String) {
    myFixture.configureByFile("$name.bazelrc")
    myFixture.doHighlighting()

    myFixture.launchAction(myFixture.findSingleIntention(""))

    myFixture.checkResultByFile("${name}Result.bazelrc")
  }
}
