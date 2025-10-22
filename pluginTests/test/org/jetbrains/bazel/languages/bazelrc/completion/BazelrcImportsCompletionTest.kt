package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelrcImportsCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "<caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll BazelImportCompletionProvider.importKeywordAndDescriptions.keys
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """im<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

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
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("text.bazelrc", "dir", ".bazelrc")
  }
}
