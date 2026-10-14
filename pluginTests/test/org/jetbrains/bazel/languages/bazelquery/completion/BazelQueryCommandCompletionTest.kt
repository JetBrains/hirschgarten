package org.jetbrains.bazel.languages.bazelquery.completion

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.common.timeoutRunBlocking
import io.kotest.matchers.collections.shouldContainAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelQueryCommandCompletionTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `beginning of line should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", "<caret>")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownCommands
  }

  @Test
  fun `inside command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", """a<caret>""")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll listOf("attr()", "allrdeps()", "allpaths()")
  }

  @Test
  fun `inside double quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", """"<caret>""")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownCommands
  }

  @Test
  fun `inside single quoted command should complete commands`() {
    // given
    myFixture.configureByText(".bazelquery", "'<caret>'")

    // when
    val lookups = completeBasic()

    // then
    lookups shouldContainAll knownCommands
  }

  private fun completeBasic(): List<String> = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      myFixture.completeBasic().flatMap { it.allLookupStrings }
    }
  }
}
