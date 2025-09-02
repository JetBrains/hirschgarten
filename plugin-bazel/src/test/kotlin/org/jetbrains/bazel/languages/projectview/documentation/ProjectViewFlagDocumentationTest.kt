package org.jetbrains.bazel.languages.projectview.documentation

import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkDocumentationTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProjectViewFlagDocumentationTest : StarlarkDocumentationTestCase() {
  @Test
  fun `should show documentation for build flags`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      build_flags: 
        --action_cache<caret>_store_output_metadata
      """.trimIndent(),
    )

    val documentation = getDocumentationAtCaret()
    assertNotNull(documentation)
    assertTrue(documentation!!.contains("action_cache_store_output_metadata"))
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

    val documentation = getDocumentationAtCaret()
    assertNull(documentation)
  }
}
