package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelrcConfigCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `should not suggest any config if none exists`() {
    // given
    myFixture.configureByText(
      ".bazelrc",
      """
      |info flag
      |common flag
      |debug flag
      |
      |info:<caret> flag
      """.trimMargin(),
    )

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldHaveSize 0
  }

  @Test
  fun `should uniquely suggest existing configs`() {
    // given
    myFixture.configureByText(
      ".bazelrc",
      """
      |info:config1 flag
      |common:config1 flag
      |debug:config2 flag
      |
      |info:<caret> flag
      """.trimMargin(),
    )

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainOnly listOf("config1", "config2")
  }

  @Test
  fun `should suggest configs from imports`() {
    // given
    myFixture.createFile(".deep.bazelrc", "info:deep-config")
    myFixture.createFile(
      ".immediate.bazelrc",
      """
        |import .deep.bazelrc
        |
        |info:immediate
      """.trimMargin(),
    )
    myFixture.configureByText(
      ".test.bazelrc",
      """
      |import .immediate.bazelrc
      |
      |info:<caret>
      """.trimMargin(),
    )

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainOnly listOf("deep-config", "immediate")
  }
}
