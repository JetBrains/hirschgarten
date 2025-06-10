package org.jetbrains.bazel.languages.bazelquery.completion

import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelQueryCommandCompletionTest : BazelQueryCompletionTestCase() {
  override fun getTestDataPath(): String = "plugin-bazel/src/test/testData"

  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", "<caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownCommands
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", """a<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll listOf("attr()", "allrdeps()", "allpaths()")
  }

  @Test
  fun `inside double quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", """"<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownCommands
  }

  @Test
  fun `inside single quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", "'<caret>'")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownCommands
  }
}
