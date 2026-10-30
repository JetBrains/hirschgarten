package org.jetbrains.bazel.languages.projectview.documentation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import org.jetbrains.bazel.languages.starlark.fixtures.documentationAtCaret
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProjectViewFlagDocumentationTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `should show documentation for build flags`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      build_flags: 
        --action_cache<caret>_store_output_metadata
      """.trimIndent(),
    )

    val documentation = myFixture.documentationAtCaret()
    documentation.shouldNotBeNull()
    documentation shouldContain "action_cache_store_output_metadata"
  }

  @Test
  fun `should not show documentation for invalid flag`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      build_flags: 
        --not_a_fl<caret>ag
      """.trimIndent(),
    )

    val documentation = myFixture.documentationAtCaret()
    documentation.shouldBeNull()
  }
}
