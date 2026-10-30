package org.jetbrains.bazel.languages.projectview.psi

import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightProjectFixture
import com.intellij.testFramework.common.timeoutRunBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@BazelTestApplication
class ProjectViewCompilationModeTest {

  private val projectFixture = codeInsightProjectFixture()
  private val codeInsightFixture by codeInsightFixture(projectFixture)

  @Test
  fun testCreateBuildFlagsSection() {
    checkSetCompilationMode(
      before = """
      directories:
        .
      """,
      after = """
      directories:
        .
      build_flags:
        --compilation_mode=dbg
      """,
    )
  }

  @Test
  fun testCreateBuildFlagsSectionWithoutTrailingNewline() {
    checkSetCompilationMode(
      before = "directories: .",
      after = """
      directories: .
      build_flags:
        --compilation_mode=dbg
      """,
      inputEndsWithNewLine = false,
    )
  }

  @Test
  fun testAppendToBuildFlagsSection() {
    checkSetCompilationMode(
      before = """
      build_flags:
        --keep_going
      directories:
        .
      """,
      after = """
      build_flags:
        --keep_going
        --compilation_mode=dbg
      directories:
        .
      """,
    )
  }

  @Test
  fun testReplaceCompilationModeFlags() {
    checkSetCompilationMode(
      before = """
      build_flags:
        -c opt
        --keep_going
        --compilation_mode=fastbuild
        -c=opt
      """,
      after = """
      build_flags:
        --keep_going
        --compilation_mode=dbg
      """,
    )
  }

  @Test
  fun testReplaceOnlyCompilationModeFlag() {
    checkSetCompilationMode(
      before = """
      build_flags:
        --compilation_mode=opt
      """,
      after = """
      build_flags:
        --compilation_mode=dbg
      """,
    )
  }

  @Test
  fun testBuildFlagsEndOffset() {
    val text = """
      build_flags:
        --keep_going
      directories:
        .
      """.trimIndent() + "\n"

    assertThat(buildFlagsEndOffset(text)).isEqualTo(text.indexOf("--keep_going") + "--keep_going".length)
  }

  @Test
  fun testBuildFlagsEndOffsetWithoutSection() {
    val text = "directories:\n  .\n"

    assertThat(buildFlagsEndOffset(text)).isEqualTo(text.length)
  }

  private fun buildFlagsEndOffset(text: String): Int = timeoutRunBlocking(30.seconds) {
    withContext(Dispatchers.EDT) {
      val file = codeInsightFixture.configureByText(".bazelproject", text) as ProjectViewPsiFile
      file.buildFlagsEndOffset()
    }
  }

  private fun checkSetCompilationMode(
    before: String,
    after: String,
    inputEndsWithNewLine: Boolean = true,
  ) {
    timeoutRunBlocking(30.seconds) {
      withContext(Dispatchers.EDT) {
        val file = codeInsightFixture.configureByText(
          ".bazelproject",
          before.trimIndent() + if (inputEndsWithNewLine) "\n" else "",
        ) as ProjectViewPsiFile

        WriteCommandAction.runWriteCommandAction(codeInsightFixture.project) {
          file.setCompilationMode("dbg")
        }

        codeInsightFixture.checkResult(after.trimIndent() + "\n")
      }
    }
  }
}
