package org.jetbrains.bazel.languages.projectview.quickfix

import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class ReplaceWithIndexQuickFixTest {
  private val codeInsightFixture by bazelCodeInsightFixture()

  @Test
  fun `should replace index_all_files_in_directories with a match-all pattern`() {
    checkQuickFix(
      before = """
      index_all_files<caret>_in_directories: true
      """,
      after = """
      index: *
      """,
    )
  }

  @Test
  fun `should move the patterns of index_additional_files_in_directories`() {
    checkQuickFix(
      before = """
      index_additional_files<caret>_in_directories:
        *.xml
        package.json
      """,
      after = """
      index:
        *.xml
        package.json
      """,
    )
  }

  @Test
  fun `should replace both deprecated sections when invoked on index_all_files_in_directories`() {
    checkQuickFix(
      before = """
      index_additional_files_in_directories:
        *.xml
      index_all_files<caret>_in_directories: true
      """,
      after = """
      index: *
      """,
    )
  }

  @Test
  fun `should replace both deprecated sections when invoked on index_additional_files_in_directories`() {
    checkQuickFix(
      before = """
      index_all_files_in_directories: false
      index_additional_files<caret>_in_directories:
        *.xml
      """,
      after = """
      index:
        *.xml
      """,
    )
  }

  @Test
  fun `should only remove the deprecated sections when an index section exists`() {
    checkQuickFix(
      before = """
      index:
        docs/*
      index_additional_files<caret>_in_directories:
        *.xml
      """,
      after = """
      index:
        docs/*
      """,
    )
  }

  @Test
  fun `should replace an empty index section`() {
    checkQuickFix(
      before = """
      index:
      index_additional_files<caret>_in_directories:
        *.xml
      """,
      after = """
      index:
        *.xml
      """,
    )
  }

  @Test
  fun `should remove index_all_files_in_directories when it is false`() {
    checkQuickFix(
      before = """
      directories: .
      index_all_files<caret>_in_directories: false
      """,
      after = """
      directories: .
      """,
    )
  }

  @Test
  fun `should keep the not indented comment after the replaced section`() {
    checkQuickFix(
      before = """
      index_additional_files<caret>_in_directories:
        *.xml

      # the targets
      targets:
        //...
      """,
      after = """
      index:
        *.xml

      # the targets
      targets:
        //...
      """,
    )
  }

  @Test
  fun `should keep the not indented comment after the removed sections`() {
    checkQuickFix(
      before = """
      index_all_files_in_directories: false
      # the additional files
      index_additional_files<caret>_in_directories:
        *.xml
      # the targets
      targets:
        //...
      """,
      after = """
      # the additional files
      index:
        *.xml
      # the targets
      targets:
        //...
      """,
    )
  }

  private fun checkQuickFix(before: String, after: String) = codeInsightFixture.checkQuickFix(
    name = BazelProjectViewBundle.message("quickfix.deprecated.section.replace.presentation", "index"),
    before = before,
    after = after,
  )
}
