package org.jetbrains.bazel.languages.bazelrc.quickfix

import org.jetbrains.bazel.config.BazelPluginBundle
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
  fun testDeleteNoOpFlag() =
    doTest("DeleteNoOpFlag", BazelPluginBundle.message("quickfix.bazelrc.delete.noop.flag.presentation"))

  @Test
  fun testReplaceDeprecatedFlag() =
    doTest("ReplaceDeprecatedFlag", renameFlagIntentionHint)

  @Test
  fun testReplaceNegatedDeprecatedFlag() =
    doTest("ReplaceNegatedDeprecatedFlag", renameFlagIntentionHint)

  private fun doTest(name: String, intentionName: String) {
    myFixture.configureByFile("$name.bazelrc")
    myFixture.doHighlighting()

    myFixture.launchAction(myFixture.findSingleIntention(intentionName))

    myFixture.checkResultByFile("${name}Result.bazelrc")
  }
}

private val renameFlagIntentionHint =
  BazelPluginBundle.message("quickfix.bazelrc.rename.flag.presentation", "incompatible_allow_tags_propagation")
