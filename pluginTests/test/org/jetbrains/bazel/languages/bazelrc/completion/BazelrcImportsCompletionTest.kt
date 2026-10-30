package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcImportsCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "<caret>")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainAll BazelImportCompletionProvider.importKeywordAndDescriptions.keys
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """im<caret>""")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainExactlyInAnyOrder BazelImportCompletionProvider.importKeywordAndDescriptions.keys
  }

  @Test
  fun `should complete relatively to current file`() {
    // given
    myFixture.tempDirFixture.createFile("dir/.bazelrc", "")
    myFixture.tempDirFixture.createFile(".bazelrc", "")

    myFixture.configureByText("text.bazelrc", """import <caret>""")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainExactlyInAnyOrder listOf("text.bazelrc", "dir", ".bazelrc")
  }
}
