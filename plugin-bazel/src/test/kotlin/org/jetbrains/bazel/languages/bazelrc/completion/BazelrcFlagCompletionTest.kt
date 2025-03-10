package org.jetbrains.bazel.languages.bazelrc.completion

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import io.kotest.matchers.collections.shouldNotHaveSize
import org.jetbrains.bazel.languages.bazelrc.fixtures.BazelrcCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.collections.flatMap

@RunWith(JUnit4::class)
class BazelrcFlagCompletionTest : BazelrcCompletionTestCase() {
  @Test
  fun `should complete at beginning of a flag`() {
    // given
    myFixture.configureByText(".bazelrc", "common <caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldNotHaveSize 0
  }

  @Test
  fun `should complete before a value`() {
    // given
    myFixture.configureByText(".bazelrc", """common <caret>asdfa""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldNotHaveSize 0
  }

  @Test
  fun `completion should match the line command`() {
    // given
    myFixture.configureByText(".bazelrc", """startup <caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    // commands that are specific for startup
    lookups shouldContainAll listOf("--install_base", "--workspace_rc")

    // commands that are not in startup phase
    lookups shouldNotContainAnyOf listOf("--inject_repository", "--experimental_explicit_aspects")
  }

  @Test
  fun `completion should include the no variants`() {
    // given
    myFixture.configureByText(".bazelrc", """common <caret>""")

    // when
    myFixture.completeBasic()
    myFixture.type("cache")
    val lookups = myFixture.lookupElementStrings!!.toTypedArray()

    // then
    lookups shouldContainAll listOf("--action_cache", "--noaction_cache", "--cache_test_results", "--nocache_test_results")
  }
}
