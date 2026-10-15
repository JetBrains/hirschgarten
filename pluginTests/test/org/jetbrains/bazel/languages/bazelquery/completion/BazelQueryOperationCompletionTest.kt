package org.jetbrains.bazel.languages.bazelquery.completion

import io.kotest.matchers.collections.shouldContainAll
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelQueryOperationCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `beginning of line should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", "<caret>")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `inside command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", """l<caret>""")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll listOf("let  in")
  }

  @Test
  fun `inside double quoted command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", """"<caret>""")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `inside single quoted command should complete operations`() {
    // given
    myFixture.configureByText(".bazelquery", "'<caret>'")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownOperations
  }

  @Test
  fun `after command should complete infix operations`() {
    // given
    myFixture.configureByText(".bazelquery", "//my:target <caret>")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll listOf("union", "except", "intersect")
  }

  private fun completeBasic(): List<String> = myFixture.completeBasic().flatMap { it.allLookupStrings }
}
