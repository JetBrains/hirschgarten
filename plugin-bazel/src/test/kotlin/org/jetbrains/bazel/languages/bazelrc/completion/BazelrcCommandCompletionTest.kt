package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelrcCommandCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "<caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """in<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder listOf("info", "mobile-install", "print_action")
  }

  @Test
  fun `inside double quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """"<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }

  @Test
  fun `inside single quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "'<caret>'")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainExactlyInAnyOrder BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }
}
