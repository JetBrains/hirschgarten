package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.completeBasicLookupStrings
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelrcCommandCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "<caret>")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainAll BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """in<caret>""")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainExactlyInAnyOrder listOf("info", "mobile-install", "print_action")
  }

  @Test
  fun `inside double quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", """"<caret>""")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainAll BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }

  @Test
  fun `inside single quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelrc", "'<caret>'")

    // when
    val lookups = myFixture.completeBasicLookupStrings()

    // then
    lookups shouldContainAll BazelCommandCompletionProvider.knownCommandsToDescriptions.keys
  }
}
