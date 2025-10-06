package org.jetbrains.bazel.languages.bazelquery.completion

import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.languages.bazelquery.fixtures.BazelQueryCompletionTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelQueryOperationCompletionTest : BazelQueryCompletionTestCase() {
  @Test
  fun `beginning of line should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", "<caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `inside command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", """l<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll listOf("let  in")
  }

  @Test
  fun `inside double quoted command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", """"<caret>""")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `inside single quoted command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", "'<caret>'")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `after command should complete infix operations`() {
    // given
    myFixture.configureByText(".bazelquery", "//my:target <caret>")

    // when
    val lookups = myFixture.completeBasic().flatMap { it.allLookupStrings }

    // then
    lookups shouldContainAll listOf("union", "except", "intersect")
  }
}
