package org.jetbrains.bazel.languages.projectview.quickfix

import com.intellij.openapi.application.EDT
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class MergeIntoSectionQuickFixTest {
  private val projectFixture = projectFixture(openAfterCreation = true)
  private val tempDirFixture = tempPathFixture()
  private val moduleFixture = projectFixture.moduleFixture(tempDirFixture, addPathToSourceRoot = true)
  private val codeInsightFixture by codeInsightFixture(projectFixture, tempDirFixture)

  @BeforeEach
  fun setUp() {
    moduleFixture.get()
  }

  @Test
  fun `should rename the section in place when there is nothing to merge with`() {
    checkQuickFix(
      before = """
      python_debug<caret>_flags:
        --run_under=valgrind
      """,
      after = """
      debug_flags:
        --run_under=valgrind
      """,
    )
  }

  @Test
  fun `should append the items to a replacement declared earlier in the file`() {
    checkQuickFix(
      before = """
      debug_flags:
        --run_under=valgrind
      python_debug<caret>_flags:
        --run_under=strace
      """,
      after = """
      debug_flags:
        --run_under=valgrind
        --run_under=strace
      """,
    )
  }

  @Test
  fun `should keep the replacement in place when it is declared later in the file`() {
    checkQuickFix(
      before = """
      python_debug<caret>_flags:
        --run_under=strace
      build_flags:
        --keep_going
      debug_flags:
        --run_under=valgrind
      """,
      after = """
      build_flags:
        --keep_going
      debug_flags:
        --run_under=valgrind
        --run_under=strace
      """,
    )
  }

  @Test
  fun `should indent the items of an inline section`() {
    checkQuickFix(
      before = """
      debug_flags:
        --run_under=valgrind
      python_debug<caret>_flags: --run_under=strace
      """,
      after = """
      debug_flags:
        --run_under=valgrind
        --run_under=strace
      """,
    )
  }

  @Test
  fun `should preserve the comments of both sections`() {
    checkQuickFix(
      before = """
      debug_flags:
        # run it under valgrind
        --run_under=valgrind
      python_debug<caret>_flags:
        # and under strace
        --run_under=strace
      """,
      after = """
      debug_flags:
        # run it under valgrind
        --run_under=valgrind
        # and under strace
        --run_under=strace
      """,
    )
  }

  @Test
  fun `should keep the blank lines and the not indented comment between the sections in place`() {
    checkQuickFix(
      before = """
      debug_flags:
        # bb
        --android_sdk


      # sdsd
      python_debug<caret>_flags:
        # aa
        --experimental_allow_runtime_deps_on_neverlink
      """,
      after = """
      debug_flags:
        # bb
        --android_sdk
        # aa
        --experimental_allow_runtime_deps_on_neverlink


      # sdsd
      """,
    )
  }

  @Test
  fun `should keep the not indented comment after the deprecated section`() {
    checkQuickFix(
      before = """
      debug_flags:
        --run_under=valgrind
      python_debug<caret>_flags:
        --run_under=strace

      # the build flags
      build_flags:
        --keep_going
      """,
      after = """
      debug_flags:
        --run_under=valgrind
        --run_under=strace

      # the build flags
      build_flags:
        --keep_going
      """,
    )
  }

  @Test
  fun `should keep the not indented comment at the end of the file after the deprecated section`() {
    checkQuickFix(
      before = """
      debug_flags:
        --run_under=valgrind
      python_debug<caret>_flags:
        --run_under=strace
      # the end of the file
      """,
      after = """
      debug_flags:
        --run_under=valgrind
        --run_under=strace
      # the end of the file
      """,
    )
  }

  @Test
  fun `should merge a deprecated section the file ends on without a newline`() {
    checkQuickFix(
      before = """
      debug_flags:
        --run_under=valgrind
      build_flags:
        --keep_going
      python_debug<caret>_flags:
        --run_under=strace
      """,
      after = """
      debug_flags:
        --run_under=valgrind
        --run_under=strace
      build_flags:
        --keep_going
      """,
      inputEndsWithNewLine = false,
    )
  }

  private fun checkQuickFix(
    before: String,
    after: String,
    inputEndsWithNewLine: Boolean = true,
  ) {
    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        codeInsightFixture.configureByText(".bazelproject", before.trimIndent() + if (inputEndsWithNewLine) "\n" else "")
        val intention =
          codeInsightFixture.findSingleIntention(
            BazelProjectViewBundle.message("quickfix.deprecated.section.replace.presentation", "debug_flags"),
          )
        codeInsightFixture.launchAction(intention)
        codeInsightFixture.checkResult(after.trimIndent() + "\n")
      }
    }
  }
}
